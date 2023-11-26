package wikixmlsplit.parser;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedExecutionException;
import wikixmlsplit.datastructures.MyPageType;
import wikixmlsplit.parser.Reader.Config;

import java.nio.file.Paths;
import java.util.concurrent.*;

/**
 * Combines wikixmlsplit.Main and wikixmlsplit.parser.Reader to avoid
 * serializing the individual page files to disk, but instead directly parses
 * them and only serializes the (filtered) AST.
 */
public class DirectParseMain extends Main {
	
	@Parameter(names = "-timeLimit")
	private int timeLimit = 10;

	public DirectParseMain() {
	}

	public static void main(String[] args) throws Exception {
		DirectParseMain main = new DirectParseMain();
		JCommander.newBuilder().addObject(main).build().parse(args);
		main.initReader();
		main.run();
	}

	private ThreadLocal<Reader> r = ThreadLocal.withInitial(this::initReader);
	private TimeLimiter limiter;
	private BoundedExecutor serv = new BoundedExecutor(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()), Runtime.getRuntime().availableProcessors() * 5);

	@Override
	public void run() throws Exception {
		ExecutorService executor = Executors.newCachedThreadPool();

		try {
			limiter = SimpleTimeLimiter.create(executor);
			super.run();
		} finally {
			serv.shutdown();
			executor.shutdown();
		}
		
		int activeThreads = ((ThreadPoolExecutor) executor).getActiveCount();
		if(activeThreads > 0) {
			System.err.println("Exiting even " + activeThreads + "active threads still running.");
		}
		System.exit(0);
	}
	
	public class BoundedExecutor {
	    private final ExecutorService exec;
	    private final Semaphore semaphore;

	    public BoundedExecutor(ExecutorService exec, int bound) {
	        this.exec = exec;
	        this.semaphore = new Semaphore(bound);
	    }

	    public void submit(final Runnable command)
	            throws InterruptedException, RejectedExecutionException {
	        semaphore.acquire();
	        try {
	            exec.execute(() -> {
					try {
						command.run();
					} finally {
						semaphore.release();
					}
				});
	        } catch (RejectedExecutionException e) {
	            semaphore.release();
	            throw e;
	        }
	    }
	    
	    public void shutdown() {
	    	exec.shutdown();
	    	try {
				exec.awaitTermination(timeLimit * 2, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	}

	@Override
	protected void writePage(MyPageType t) throws Exception {
		serv.submit(() -> {
			try {
				limiter.runUninterruptiblyWithTimeout(() -> {
					if (!r.get().handlePage(Paths.get(outputFile), t)) {
						try {
							System.err.println("Writing page " + t.getTitle());
							super.writePage(t);
						} catch (Exception e) {
							System.err.println("Error writing page " + t.getTitle());
							e.printStackTrace();
						}
					}
				}, timeLimit, TimeUnit.MINUTES);
			} catch (TimeoutException ex) {
				System.err.println("Timeout on " + t.getTitle());
				try {
					super.writePage(t);
				} catch (Exception e) {
					System.err.println("Error writing page " + t.getTitle());
					e.printStackTrace();
				}
			} catch (UncheckedExecutionException ex) {
				System.err.println("Unchecked execution exception on " + t.getTitle());
				ex.printStackTrace();
				try {
					super.writePage(t);
				} catch (Exception e) {
					System.err.println("Error writing page " + t.getTitle());
					e.printStackTrace();
				}
			}
		});
	}

	public Reader initReader() {
		Config cfg = new Reader.Config();
		cfg.setTargetTypes(targetTypes);
		cfg.setAdditionalTypes(additionalTypes);
		return new Reader(cfg);
	}
}

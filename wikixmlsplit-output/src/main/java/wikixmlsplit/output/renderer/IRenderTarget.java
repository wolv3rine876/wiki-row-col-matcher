package wikixmlsplit.output.renderer;

import wikixmlsplit.datastructures.MyRevisionType;

import java.util.List;
import java.util.Map;

public interface IRenderTarget {

	void configureObjectStore(boolean weighted, double limit1, double limit2, double limit3,
			double relaxLimit);

	List<RenderResult> track(MyRevisionType r, Map<String, List<String>> nodes);

}

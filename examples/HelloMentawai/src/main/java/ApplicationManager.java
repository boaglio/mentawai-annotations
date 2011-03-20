import org.mentawai.annotations.ApplicationManagerWithAnnotations;
import org.mentawai.core.Context;

public class ApplicationManager extends ApplicationManagerWithAnnotations {


	public void init(Context application) {

      // ativa exibir a action cadastrada em http://localhost:8080/HelloMentawai/stats.mtw
	  setStatsMode(true);

	  // busca classes com anottation no pacote examples
      setResources("examples");

	}
}


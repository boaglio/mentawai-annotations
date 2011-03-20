package examples.helloworld.actions;

import org.mentawai.annotations.core.ActionClass;
import org.mentawai.annotations.core.ConsequenceOutput;
import org.mentawai.annotations.type.ConsequenceType;
import org.mentawai.core.BaseAction;

@ActionClass( prefix = "/HelloWorld" ,
		      outputs={
		                @ConsequenceOutput(result=BaseAction.SUCCESS, page="hello.jsp",type=ConsequenceType.FORWARD),
		                @ConsequenceOutput(result=BaseAction.ERROR, page="username.jsp",type=ConsequenceType.FORWARD)
		              }
)
public class HelloMentawai extends BaseAction {

	public String execute() throws Exception {
		String username = input.getString("username");
		if (username == null || username.trim().equals("")) {
			return ERROR;
		}
		output.setValue("username", username.toUpperCase());
		return SUCCESS;
	}

}



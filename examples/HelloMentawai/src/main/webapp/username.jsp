<%@ taglib uri="http://www.mentaframework.org/tags-mtw/" prefix="mtw" %>
<h1>Hello Mentawai with Annotations!</h1>
<mtw:form>
Please type your username: <mtw:input name="username" size="25" />
<mtw:submit value="Send" action="/HelloWorld.mtw" method="post" />
</mtw:form>

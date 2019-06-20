<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Footer for home page
--%>

<%@page import="org.dspace.core.ConfigurationManager"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="org.dspace.eperson.EPerson"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.core.NewsManager" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.app.webui.util.LocaleUIHelper" %>

<%
    String footerNews = NewsManager.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-footer.html"));
    String sidebar = (String) request.getAttribute("dspace.layout.sidebar");
    String[] mlinks = new String[0];
    String mlinksConf = ConfigurationManager.getProperty("cris", "navbar.cris-entities");
    if (StringUtils.isNotBlank(mlinksConf)) {
        mlinks = StringUtils.split(mlinksConf, ",");
    }

    boolean showCommList = ConfigurationManager.getBooleanProperty("community-list.show.all", true);
    boolean isRtl = StringUtils.isNotBlank(LocaleUIHelper.ifLtr(request, "", "rtl"));
%>

<%-- Right-hand side bar if appropriate --%>
<%
    if (sidebar != null) {
%>
</div>
<div class="col-md-3">
    <%= sidebar%>
</div>
</div>
<%
    }
%>
</div>
<br/>
</main>
<%-- Page footer --%>
<footer class="navbar navbar-inverse navbar-bottom navbar-square">
    <div class="container-fluid extra-footer row">
        <div id="footer_feedback" class="col-sm-4 pull-<%= isRtl ? "right":"left" %>">
           <a href="<%= request.getContextPath() %>/feedback"><fmt:message key="jsp.layout.footer-default.feedback"/></a>
      </div>
         <div id="designedby" class="col-sm-8 text-<%= isRtl ? "left": "right" %>">
           <fmt:message key="jsp.layout.footer-default.text"/> - 
           <fmt:message key="jsp.layout.footer-default.version-by"/> 
           <a href="http://www.4science.it/en/dspace-and-dspace-cris-services/">
               <img src="<%= request.getContextPath() %>/image/logo-4science-small.png"
                          alt="Logo 4SCIENCE" height="32px"/></a>
      </div>
  </div>
</footer>
</body>
</html>

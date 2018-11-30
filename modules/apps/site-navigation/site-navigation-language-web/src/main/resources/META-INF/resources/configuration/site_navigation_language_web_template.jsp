<%--
/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
--%>

<%@ include file="/init.jsp" %>

<%
SiteNavigationLanguageWebTemplateConfiguration siteNavigationLanguageWebTemplateConfiguration = (SiteNavigationLanguageWebTemplateConfiguration)request.getAttribute(SiteNavigationLanguageWebTemplateConfiguration.class.getName());

String currentTemplateKey = siteNavigationLanguageWebTemplateConfiguration.ddmTemplateKey();
%>

<aui:select label='<%= HtmlUtil.escape(LanguageUtil.get(request, "language-selection-style")) %>' name="ddmTemplateKey" value="<%= currentTemplateKey %>">

	<%
	long groupId = 0;
	Group companyGroup = GroupLocalServiceUtil.fetchCompanyGroup(company.getCompanyId());

	if (companyGroup != null) {
		groupId = companyGroup.getGroupId();
	}

	List<DDMTemplate> ddmTemplates = DDMTemplateLocalServiceUtil.getTemplates(groupId, PortalUtil.getClassNameId(LanguageEntry.class));

	for (DDMTemplate ddmTemplate : ddmTemplates) {
		String templateKey = ddmTemplate.getTemplateKey();
	%>

		<aui:option label="<%= ddmTemplate.getName(locale) %>" selected="<%= currentTemplateKey.equals(templateKey) %>" value="<%= templateKey %>" />

	<%
	}
	%>

</aui:select>
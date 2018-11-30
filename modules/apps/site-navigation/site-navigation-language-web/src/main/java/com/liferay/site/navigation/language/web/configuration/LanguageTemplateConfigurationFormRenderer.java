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

package com.liferay.site.navigation.language.web.configuration;

import com.liferay.configuration.admin.display.ConfigurationFormRenderer;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLocalService;
import com.liferay.frontend.taglib.servlet.taglib.util.JSPRenderer;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Drew Brokke
 */
@Component(
	configurationPid = "com.liferay.site.navigation.language.web.configuration.SiteNavigationLanguageWebTemplateConfiguration",
	immediate = true, service = ConfigurationFormRenderer.class
)
public class LanguageTemplateConfigurationFormRenderer
	implements ConfigurationFormRenderer {

	@Activate
	@Modified
	public void activate(Map<String, Object> properties) {
		_siteNavigationLanguageWebTemplateConfiguration =
			ConfigurableUtil.createConfigurable(
				SiteNavigationLanguageWebTemplateConfiguration.class,
				properties);
	}

	@Override
	public String getPid() {
		return "com.liferay.site.navigation.language.web.configuration." +
			"SiteNavigationLanguageWebTemplateConfiguration";
	}

	@Override
	public Map<String, Object> getRequestParameters(
		HttpServletRequest request) {

		Map<String, Object> params = new HashMap<>();

		String ddmTemplateKey = ParamUtil.getString(request, "ddmTemplateKey");

		params.put("ddmTemplateKey", ddmTemplateKey);

		return params;
	}

	@Override
	public void render(HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		request.setAttribute(
			SiteNavigationLanguageWebTemplateConfiguration.class.getName(),
			_siteNavigationLanguageWebTemplateConfiguration);

		_jspRenderer.renderJSP(
			_servletContext, request, response,
			"/configuration/site_navigation_language_web_template.jsp");
	}

	@Reference
	private DDMTemplateLocalService _ddmTemplateLocalService;

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private JSPRenderer _jspRenderer;

	@Reference
	private Portal _portal;

	@Reference(
		target = "(osgi.web.symbolicname=com.liferay.site.navigation.language.web)",
		unbind = "-"
	)
	private ServletContext _servletContext;

	private volatile SiteNavigationLanguageWebTemplateConfiguration
		_siteNavigationLanguageWebTemplateConfiguration;

}
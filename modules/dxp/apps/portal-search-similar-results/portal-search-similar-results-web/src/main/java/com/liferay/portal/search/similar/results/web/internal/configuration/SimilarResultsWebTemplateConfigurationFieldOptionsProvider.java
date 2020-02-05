package com.liferay.portal.search.similar.results.web.internal.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.configuration.admin.definition.ConfigurationFieldOptionsProvider;
import com.liferay.configuration.admin.definition.ConfigurationFieldOptionsProvider.Option;
import com.liferay.dynamic.data.mapping.model.DDMTemplate;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLocalService;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.ClassNameLocalService;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.search.similar.results.web.internal.display.context.SimilarResultsDocumentDisplayContext;
import com.liferay.portlet.display.template.PortletDisplayTemplate;

@Component(
	property = {
		"configuration.field.name=similarResultsTemplateKeyDefault",
		"configuration.pid=com.liferay.portal.search.similar.results.web.internal.configuration.SimilarResultsWebTemplateConfiguration"
	},
	service = ConfigurationFieldOptionsProvider.class
)
public class SimilarResultsWebTemplateConfigurationFieldOptionsProvider implements ConfigurationFieldOptionsProvider {

public List<Option> getOptions() {
	
	// this junk don't work for real
	// long groupId = 20129;
	long classNameId = _classNameLocalService.getClassNameId(SimilarResultsDocumentDisplayContext.class);
	//this fails to get 
	// get all groups
	List<Group> groups = _groupLocalService.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
	
	for (Group group : groups) {

		long groupId = group.getGroupId();
		List<Long> groupIds = new ArrayList<>();
		groupIds.add(groupId);
	}

	List<DDMTemplate> templates = _ddmTemplateLocalService.getTemplates(groupId, classNameId);
	Stream<DDMTemplate> stream = templates.stream();
			
//			_portletDisplayTemplate.getPortletDisplayTemplateDDMTemplate
//			(groupId, classNameId, "displayStyle");
	
	
	return stream.map(
		template -> new Option() {

			@Override
			public String getLabel(Locale locale) {
				return template.getNameCurrentValue();
			}

			@Override
			public String getValue() {
				return template.getTemplateKey();
			}
		}
		).collect(
		Collectors.toList()
	);
}

	@Reference(unbind = "-")
	protected void setPortletDisplayTemplate(
		PortletDisplayTemplate portletDisplayTemplate) {

		_portletDisplayTemplate = portletDisplayTemplate;
	}

	@Reference(unbind = "-")
	protected void setClassNameLocalService(
		ClassNameLocalService classNameLocalService) {

		_classNameLocalService = classNameLocalService;
	}
	@Reference(unbind = "-")
	protected void setDDMTemplateLocalService(
		DDMTemplateLocalService ddmTemplateLocalService) {

		_ddmTemplateLocalService = ddmTemplateLocalService;
	}

	@Reference(unbind = "-")
	protected void setGroupLocalService(
		GroupLocalService groupLocalService) {

		_groupLocalService = groupLocalService;
	}

	private static PortletDisplayTemplate _portletDisplayTemplate;
	private static ClassNameLocalService _classNameLocalService;
	private static GroupLocalService _groupLocalService;
	private static DDMTemplateLocalService _ddmTemplateLocalService;
}

package com.liferay.portal.search.similar.results.web.internal.configuration;

import java.util.List;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;

import com.liferay.asset.kernel.AssetRendererFactoryRegistryUtil;
import com.liferay.asset.kernel.model.AssetRendererFactory;
import com.liferay.configuration.admin.definition.ConfigurationFieldOptionsProvider;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.search.similar.results.web.internal.constants.SimilarResultsPortletKeys;

@Component(
	property = {
		"configuration.field.name=enabledClassNames",
		"configuration.pid=com.liferay.asset.auto.tagger.google.cloud.natural.language.internal.configuration.GCloudNaturalLanguageAssetAutoTaggerCompanyConfiguration",
		"configuration.pid=com.liferay.asset.auto.tagger.opennlp.internal.configuration.OpenNLPDocumentAssetAutoTaggerCompanyConfiguration"
	},
	service = ConfigurationFieldOptionsProvider.class
)
public class SimilarResultsWebTemplateConfigurationFieldOptionsProvider implements ConfigurationFieldOptionsProvider {

public List<Option> getOptions() {
	
	
	// get the portlet id
	String id = SimilarResultsPortletKeys.SIMILAR_RESULTS;
	
	
	
List<AssetRendererFactory<?>> assetRendererFactories =
		AssetRendererFactoryRegistryUtil.getAssetRendererFactories(
			CompanyThreadLocal.getCompanyId());

	Stream<AssetRendererFactory<?>> stream =
		assetRendererFactories.stream();

	return stream.filter(
		assetRendererFactory -> {
			TextExtractor textExtractor =
				_textExtractorTracker.getTextExtractor(
					assetRendererFactory.getClassName());

			return textExtractor != null;
		}
	).map(
		assetRendererFactory -> new Option() {

			@Override
			public String getLabel(Locale locale) {
				return assetRendererFactory.getTypeName(locale);
			}

			@Override
			public String getValue() {
				return assetRendererFactory.getClassName();
			}

		}
	).collect(
		Collectors.toList()
	);
}

}

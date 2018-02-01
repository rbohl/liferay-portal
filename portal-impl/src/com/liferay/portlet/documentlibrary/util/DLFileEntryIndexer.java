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

package com.liferay.portlet.documentlibrary.util;

import com.liferay.document.library.kernel.exception.NoSuchFileVersionException;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.model.DLFileEntryMetadata;
import com.liferay.document.library.kernel.model.DLFileVersion;
import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.document.library.kernel.service.DLFileEntryLocalServiceUtil;
import com.liferay.document.library.kernel.service.DLFileEntryMetadataLocalServiceUtil;
import com.liferay.document.library.kernel.service.DLFolderLocalServiceUtil;
import com.liferay.dynamic.data.mapping.kernel.DDMFormValues;
import com.liferay.dynamic.data.mapping.kernel.DDMStructure;
import com.liferay.dynamic.data.mapping.kernel.DDMStructureManager;
import com.liferay.dynamic.data.mapping.kernel.DDMStructureManagerUtil;
import com.liferay.dynamic.data.mapping.kernel.StorageEngineManagerUtil;
import com.liferay.expando.kernel.model.ExpandoBridge;
import com.liferay.expando.kernel.util.ExpandoBridgeFactoryUtil;
import com.liferay.expando.kernel.util.ExpandoBridgeIndexerUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.portal.kernel.comment.Comment;
import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.IndexableActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.search.BaseIndexer;
import com.liferay.portal.kernel.search.BaseRelatedEntryIndexer;
import com.liferay.portal.kernel.search.BooleanClauseOccur;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.DocumentHelper;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.IndexWriterHelperUtil;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.RelatedEntryIndexer;
import com.liferay.portal.kernel.search.RelatedEntryIndexerRegistryUtil;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.search.Summary;
import com.liferay.portal.kernel.search.filter.BooleanFilter;
import com.liferay.portal.kernel.search.filter.QueryFilter;
import com.liferay.portal.kernel.search.generic.BooleanQueryImpl;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.spring.osgi.OSGiBeanProperties;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.repository.liferayrepository.model.LiferayFileEntry;
import com.liferay.portal.util.PrefsPropsUtil;
import com.liferay.portal.util.PropsValues;
import com.liferay.portlet.documentlibrary.service.permission.DLFileEntryPermission;
import com.liferay.trash.kernel.util.TrashUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

/**
 * @author Brian Wing Shun Chan
 * @author Raymond Augé
 * @author Alexander Chow
 */

@OSGiBeanProperties(
	property = {
		"related.entry.indexer.class.name=com.liferay.document.library.kernel.model.DLFileEntry"
	},
	service = {Indexer.class, RelatedEntryIndexer.class}
)

/** 
 * When does a dev want to implement RelatedEntryIndexer?
 */
public class DLFileEntryIndexer
	extends BaseIndexer<DLFileEntry> implements RelatedEntryIndexer {

	public static final String CLASS_NAME = DLFileEntry.class.getName();

/** No changes to the constructor, but let's run through it anyway
 * 
 *  setDefaultSelectedFieldNames: specifies the fields used to retrieve hits from 
 *  the search engine documents.
 *  
 * setFilterSearch: i've never really understood this, but here's what I wrote in 
 * the 7.0 docs: enabling a document-by-document check of the search results’ VIEW 
 * permissions. This is redundant most of the time, but safeguards against unexpected 
 * problems like the search index becoming stale, or if permission inheritance 
 * doesn’t happen fast enough. Most of Liferay Portal’s internal apps use this setting. If not set, the indexer relies on the permissions information indexed in the search engine.
 * 
 * setPermissionAware: checks the users permission on the resource before returning 
 * search documents
 *  
 *  In general, the constructor of each indexer is called when the portal is started. 
 *  I guess the object waits in memory for its methods to be called, at query time or 
 *  at index time? But I might have distorted basic Java concepts here.
 *  
 *  
 *  */
	public DLFileEntryIndexer() {
		setDefaultSelectedFieldNames(
			Field.ASSET_TAG_NAMES, Field.COMPANY_ID, Field.CONTENT,
			Field.ENTRY_CLASS_NAME, Field.ENTRY_CLASS_PK, Field.GROUP_ID,
			Field.MODIFIED_DATE, Field.SCOPE_GROUP_ID, Field.TITLE, Field.UID);
		setFilterSearch(true);
		setPermissionAware(true);
	}
	

	/** 
	 * addRelatedClassNames is used during construction of the query, called in 
	 * postProcessContextBooleanFilter. It appears to be used to determine whether 
	 * to query attachments. I guess if isIncludeAttachments is false, the title and
	 * description fields are queried but not the content? That's a guess.
	 * 
	 * 
	 * 
	 */
	@Override
	public void addRelatedClassNames(
			BooleanFilter contextBooleanFilter, SearchContext searchContext)
		throws Exception {

		_relatedEntryIndexer.addRelatedClassNames(
			contextBooleanFilter, searchContext);
	}

/** 
 * addRelatedEntryFields is called in dogetDocument, which is where we mess 
 * with the document itself, but it's in an if block I don't really understand.
 * 	
 */
	@Override
	public void addRelatedEntryFields(Document document, Object obj)
		throws Exception {

		Comment comment = (Comment)obj;

		FileEntry fileEntry = null;

		try {
			fileEntry = DLAppLocalServiceUtil.getFileEntry(
				comment.getClassPK());
		}
		catch (Exception e) {
			return;
		}

		if (fileEntry instanceof LiferayFileEntry) {
			DLFileEntry dlFileEntry = (DLFileEntry)fileEntry.getModel();

			document.addKeyword(Field.FOLDER_ID, dlFileEntry.getFolderId());
			document.addKeyword(Field.HIDDEN, dlFileEntry.isInHiddenFolder());
			document.addKeyword(
				Field.TREE_PATH,
				StringUtil.split(dlFileEntry.getTreePath(), CharPool.SLASH));
		}
	}

	@Override
	public String getClassName() {
		return CLASS_NAME;
	}

	
/**
 * hasPermission is where the dev specifies which resource permission (usually VIEW) 
 * should be used when permissions filtering this entity for the search results?
 * 	
 */
	@Override
	public boolean hasPermission(
			PermissionChecker permissionChecker, String entryClassName,
			long entryClassPK, String actionId)
		throws Exception {

		return DLFileEntryPermission.contains(
			permissionChecker, entryClassPK, ActionKeys.VIEW);
	}

/** 
 * isVisible probably has something to do with checking the workflow status of the
 * entity and determining if it should be displayed (if it's pending, or in_trash,
 * for example, it shouldn't be displayed). What happens if you don't implement this
 * method and your entity is workflow-enabled?
 * 	
 */
	@Override
	public boolean isVisible(long classPK, int status) throws Exception {
		FileEntry fileEntry = DLAppLocalServiceUtil.getFileEntry(classPK);

		FileVersion fileVersion = fileEntry.getFileVersion();

		return isVisible(fileVersion.getStatus(), status);
	}

/**
 * isvisibleRelatedEntry now comes from the interface RelatedEntryIndexer. 
 * I don't really know what this does, other than return a boolean.
 */
	@Override
	public boolean isVisibleRelatedEntry(long classPK, int status)
		throws Exception {

		try {
			FileEntry fileEntry = DLAppLocalServiceUtil.getFileEntry(classPK);

			if (fileEntry instanceof LiferayFileEntry) {
				DLFileEntry dlFileEntry = (DLFileEntry)fileEntry.getModel();

				if (dlFileEntry.isInHiddenFolder()) {
					Indexer<?> indexer = IndexerRegistryUtil.getIndexer(
						dlFileEntry.getClassName());

					return indexer.isVisible(dlFileEntry.getClassPK(), status);
				}
			}
		}
		catch (Exception e) {
			if (_log.isInfoEnabled()) {
				_log.info("Unble to get file entry", e);
			}

			return false;
		}

		return true;
	}

/**
 * 	postProcessContextBooleanFilter is invoked while the main search query 
 *  is being constructed.    
 *  
 */
	@Override
	public void postProcessContextBooleanFilter(
			BooleanFilter contextBooleanFilter, SearchContext searchContext)
		throws Exception {

	/**   
	 * 
	 * addStatus, which comes from BaseIndexer, makes sure  entities with
	 * STATUS_IN_TRASH are excluded from the query. [How is that different from
	 * isVisible?]
	 * 
	*/
		
		addStatus(contextBooleanFilter, searchContext);

		
	/** 
	 * calls addRelatedClassNames is the search context has isIncludeAttachments=true
	 * 	
	 */
		
		if (searchContext.isIncludeAttachments()) {
			addRelatedClassNames(contextBooleanFilter, searchContext);
		}

	/** 
	 * Adds the HIDDEN field if it can't get any folder IDs or if it only get the 
	 * default parent folder id (not exactly sure what that is)
	 * 	
	 */
		
		if (ArrayUtil.isEmpty(searchContext.getFolderIds()) ||
			ArrayUtil.contains(
				searchContext.getFolderIds(),
				DLFolderConstants.DEFAULT_PARENT_FOLDER_ID)) {

			contextBooleanFilter.addRequiredTerm(
				Field.HIDDEN, searchContext.isIncludeAttachments());
		}

	/** 
	 * hmm, not sure what addSearchClassTypeIds does, even after inspecting it 
	 * in BaseIndexer
	 * 	
	 */
		
		addSearchClassTypeIds(contextBooleanFilter, searchContext);

		String ddmStructureFieldName = (String)searchContext.getAttribute(
			"ddmStructureFieldName");
		Serializable ddmStructureFieldValue = searchContext.getAttribute(
			"ddmStructureFieldValue");

		if (Validator.isNotNull(ddmStructureFieldName) &&
			Validator.isNotNull(ddmStructureFieldValue)) {

			String[] ddmStructureFieldNameParts = StringUtil.split(
				ddmStructureFieldName,
				DDMStructureManager.STRUCTURE_INDEXER_FIELD_SEPARATOR);

			DDMStructure ddmStructure = DDMStructureManagerUtil.getStructure(
				GetterUtil.getLong(ddmStructureFieldNameParts[2]));

			String fieldName = StringUtil.replaceLast(
				ddmStructureFieldNameParts[3],
				StringPool.UNDERLINE.concat(
					LocaleUtil.toLanguageId(searchContext.getLocale())),
				StringPool.BLANK);

			try {
				ddmStructureFieldValue =
					DDMStructureManagerUtil.getIndexedFieldValue(
						ddmStructureFieldValue,
						ddmStructure.getFieldType(fieldName));
			}
			catch (Exception e) {
				if (_log.isDebugEnabled()) {
					_log.debug(e, e);
				}
			}

			BooleanQuery booleanQuery = new BooleanQueryImpl();

			booleanQuery.addRequiredTerm(
				ddmStructureFieldName,
				StringPool.QUOTE + ddmStructureFieldValue + StringPool.QUOTE);

			contextBooleanFilter.add(
				new QueryFilter(booleanQuery), BooleanClauseOccur.MUST);
		}

		String[] mimeTypes = (String[])searchContext.getAttribute("mimeTypes");

		if (ArrayUtil.isNotEmpty(mimeTypes)) {
			BooleanFilter mimeTypesBooleanFilter = new BooleanFilter();

			for (String mimeType : mimeTypes) {
				mimeTypesBooleanFilter.addTerm(
					"mimeType",
					StringUtil.replace(
						mimeType, CharPool.FORWARD_SLASH, CharPool.UNDERLINE));
			}

			contextBooleanFilter.add(
				mimeTypesBooleanFilter, BooleanClauseOccur.MUST);
		}
	}

/** 
 * postProcessSearchQuery adds clauses to the ongoing search query. When is this
 * called versus postProcessContextBooleanFilter? Probably showing my ignorance, but 
 * what's the difference between the two?
 * 
 * How do you make the decision about what to add here? I see This adds the localized
 * content field, and a bunch of others.
 */
	
	@Override
	public void postProcessSearchQuery(
			BooleanQuery searchQuery, BooleanFilter fullQueryBooleanFilter,
			SearchContext searchContext)
		throws Exception {

		String keywords = searchContext.getKeywords();

		if (Validator.isNull(keywords)) {
			addSearchTerm(searchQuery, searchContext, Field.DESCRIPTION, false);
			addSearchTerm(searchQuery, searchContext, Field.TITLE, false);
			addSearchTerm(searchQuery, searchContext, Field.USER_NAME, false);
		}

		addSearchTerm(searchQuery, searchContext, "ddmContent", false);
		addSearchTerm(searchQuery, searchContext, "extension", false);
		addSearchTerm(searchQuery, searchContext, "fileEntryTypeId", false);
		addSearchTerm(searchQuery, searchContext, "path", false);
		addSearchLocalizedTerm(
			searchQuery, searchContext, Field.CONTENT, false);

		LinkedHashMap<String, Object> params =
			(LinkedHashMap<String, Object>)searchContext.getAttribute("params");

		if (params != null) {
			String expandoAttributes = (String)params.get("expandoAttributes");

			if (Validator.isNotNull(expandoAttributes)) {
				addSearchExpando(searchQuery, searchContext, expandoAttributes);
			}
		}
	}

/** 
 * No idea, except that this is part of implementing REI
 * 	
 */
	
	@Override
	public void updateFullQuery(SearchContext searchContext) {
		if (searchContext.isIncludeAttachments()) {
			searchContext.addFullQueryEntryClassName(
				DLFileEntry.class.getName());
		}
	}

	
/**
 * 	Specific to DLFileEntries so I'm not worried about it.
 * 
 */
	
	protected void addFileEntryTypeAttributes(
			Document document, DLFileVersion dlFileVersion)
		throws PortalException {

		List<DLFileEntryMetadata> dlFileEntryMetadatas =
			DLFileEntryMetadataLocalServiceUtil.
				getFileVersionFileEntryMetadatas(
					dlFileVersion.getFileVersionId());

		for (DLFileEntryMetadata dlFileEntryMetadata : dlFileEntryMetadatas) {
			DDMFormValues ddmFormValues = null;

			try {
				ddmFormValues = StorageEngineManagerUtil.getDDMFormValues(
					dlFileEntryMetadata.getDDMStorageId());
			}
			catch (Exception e) {
			}

			if (ddmFormValues != null) {
				DDMStructureManagerUtil.addAttributes(
					dlFileEntryMetadata.getDDMStructureId(), document,
					ddmFormValues);
			}
		}
	}

/** 
 * instead of calling the BaseIndexer's createSummary in doGetSummary, 
 *  this indexer create's its own summary. not sure about that prefix field though.
 * 	
 */
	
	protected Summary createSummary(
		Locale locale, Document document, String titleField,
		String contentField) {

		String prefix = Field.SNIPPET + StringPool.UNDERLINE;

		String title = document.get(prefix + titleField, titleField);
		String content = document.get(
			locale, prefix + contentField, contentField);

		return new Summary(title, content);
	}

/** 
 * overriding doDelete just mates the DB entity with the corresponding 
 * document so the right doc is deleted when the db entity is deleted
 */
	
	@Override
	protected void doDelete(DLFileEntry dlFileEntry) throws Exception {
		deleteDocument(
			dlFileEntry.getCompanyId(), dlFileEntry.getFileEntryId());
	}

/** 
 * doGetDocument is for building the document to send to the search engine.
 * 
 */
	
	@Override
	protected Document doGetDocument(DLFileEntry dlFileEntry) throws Exception {
		if (_log.isDebugEnabled()) {
			_log.debug("Indexing document " + dlFileEntry);
		}

		boolean indexContent = true;

		InputStream is = null;

		try {
			String[] ignoreExtensions = PrefsPropsUtil.getStringArray(
				PropsKeys.DL_FILE_INDEXING_IGNORE_EXTENSIONS, StringPool.COMMA);

			if (ArrayUtil.contains(
					ignoreExtensions,
					StringPool.PERIOD + dlFileEntry.getExtension())) {

				indexContent = false;
			}

			if (indexContent) {
				DLFileVersion fileVersion = dlFileEntry.getFileVersion();

				is = fileVersion.getContentStream(false);
			}
		}
		catch (Exception e) {
			if (_log.isDebugEnabled()) {
				_log.debug("Error retrieving document stream", e);
			}
		}

		DLFileVersion dlFileVersion = dlFileEntry.getFileVersion();

		try {
			Document document = getBaseModelDocument(
				CLASS_NAME, dlFileEntry, dlFileVersion);

			if (indexContent) {
				if (is != null) {
					try {
						Locale defaultLocale = PortalUtil.getSiteDefaultLocale(
							dlFileEntry.getGroupId());

		/**
		 * Yeah! Localized content!
		 */
						String localizedField =
							LocalizationUtil.getLocalizedName(
								Field.CONTENT, defaultLocale.toString());

						document.addFile(
							localizedField, is, dlFileEntry.getTitle(),
							PropsValues.DL_FILE_INDEXING_MAX_SIZE);
					}
					catch (IOException ioe) {
						throw new SearchException(
							"Cannot extract text from file" + dlFileEntry, ioe);
					}
				}
				else if (_log.isDebugEnabled()) {
					_log.debug(
						"Document " + dlFileEntry +
							" does not have any content");
				}
			}

			document.addKeyword(
				Field.CLASS_TYPE_ID, dlFileEntry.getFileEntryTypeId());
			document.addText(Field.DESCRIPTION, dlFileEntry.getDescription());
			document.addKeyword(Field.FOLDER_ID, dlFileEntry.getFolderId());
			document.addKeyword(Field.HIDDEN, dlFileEntry.isInHiddenFolder());
			document.addText(
				Field.PROPERTIES, dlFileEntry.getLuceneProperties());

			String title = dlFileEntry.getTitle();

			if (dlFileEntry.isInTrash()) {
				title = TrashUtil.getOriginalTitle(title);
			}

			document.addText(Field.TITLE, title);

			document.addKeyword(
				Field.TREE_PATH,
				StringUtil.split(dlFileEntry.getTreePath(), CharPool.SLASH));

			document.addKeyword(
				"dataRepositoryId", dlFileEntry.getDataRepositoryId());
			document.addText(
				"ddmContent",
				extractDDMContent(dlFileVersion, LocaleUtil.getSiteDefault()));
			document.addKeyword("extension", dlFileEntry.getExtension());
			document.addKeyword(
				"fileEntryTypeId", dlFileEntry.getFileEntryTypeId());
			document.addKeyword(
				"mimeType",
				StringUtil.replace(
					dlFileEntry.getMimeType(), CharPool.FORWARD_SLASH,
					CharPool.UNDERLINE));
			document.addKeyword("path", dlFileEntry.getTitle());
			document.addKeyword("readCount", dlFileEntry.getReadCount());
			document.addKeyword("size", dlFileEntry.getSize());

			ExpandoBridge expandoBridge =
				ExpandoBridgeFactoryUtil.getExpandoBridge(
					dlFileEntry.getCompanyId(), DLFileEntry.class.getName(),
					dlFileVersion.getFileVersionId());

			ExpandoBridgeIndexerUtil.addAttributes(document, expandoBridge);

			addFileEntryTypeAttributes(document, dlFileVersion);

			if (dlFileEntry.isInHiddenFolder()) {
				List<RelatedEntryIndexer> relatedEntryIndexers =
					RelatedEntryIndexerRegistryUtil.getRelatedEntryIndexers(
						dlFileEntry.getClassName());

				if (relatedEntryIndexers != null) {
					for (RelatedEntryIndexer relatedEntryIndexer :
							relatedEntryIndexers) {

						relatedEntryIndexer.addRelatedEntryFields(
							document, new LiferayFileEntry(dlFileEntry));

						DocumentHelper documentHelper = new DocumentHelper(
							document);

						documentHelper.setAttachmentOwnerKey(
							PortalUtil.getClassNameId(
								dlFileEntry.getClassName()),
							dlFileEntry.getClassPK());

						document.addKeyword(Field.RELATED_ENTRY, true);
					}
				}
			}

			if (_log.isDebugEnabled()) {
				_log.debug("Document " + dlFileEntry + " indexed successfully");
			}

			return document;
		}
		finally {
			if (is != null) {
				try {
					is.close();
				}
				catch (IOException ioe) {
				}
			}
		}
	}

/**
 * doGetSummary returns the summary created in createSummary	
 */
	
	@Override
	protected Summary doGetSummary(
		Document document, Locale locale, String snippet,
		PortletRequest portletRequest, PortletResponse portletResponse) {

		Summary summary = createSummary(
			locale, document, Field.TITLE, Field.CONTENT);

		if (Validator.isNull(summary.getContent())) {
			summary = createSummary(document, Field.TITLE, Field.DESCRIPTION);
		}

		summary.setMaxContentLength(200);

		return summary;
	}

/**
 * doReindex is called when a reindex is triggered, from an update method call in 
 * the service layer or an explicit reindex from sys admin (soon to be search admin)	
 * 
 * calls IndexWriterHelperUtil to update the document accordingly.
 */
	
	@Override
	protected void doReindex(DLFileEntry dlFileEntry) throws Exception {
		DLFileVersion dlFileVersion = null;

		try {
			dlFileVersion = dlFileEntry.getFileVersion();
		}
		catch (NoSuchFileVersionException nsfve) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Unable to get file version for file entry " +
						dlFileEntry.getFileEntryId(),
					nsfve);
			}

			return;
		}

		if (!dlFileVersion.isApproved() && !dlFileEntry.isInTrash()) {
			return;
		}

		Document document = getDocument(dlFileEntry);

		IndexWriterHelperUtil.updateDocument(
			getSearchEngineId(), dlFileEntry.getCompanyId(), document,
			isCommitImmediately());
	}

	@Override
	protected void doReindex(String className, long classPK) throws Exception {
		DLFileEntry dlFileEntry = DLFileEntryLocalServiceUtil.getFileEntry(
			classPK);

		doReindex(dlFileEntry);
	}

	@Override
	protected void doReindex(String[] ids) throws Exception {
		if (ids.length == 1) {
			long companyId = GetterUtil.getLong(ids[0]);

			reindexFolders(companyId);
			reindexRoot(companyId);
		}
		else {
			long companyId = GetterUtil.getLong(ids[0]);
			long groupId = GetterUtil.getLong(ids[1]);
			long dataRepositoryId = GetterUtil.getLong(ids[2]);

			reindexFileEntries(companyId, groupId, dataRepositoryId);
		}
	}

	protected String extractDDMContent(
			DLFileVersion dlFileVersion, Locale locale)
		throws Exception {

		List<DLFileEntryMetadata> dlFileEntryMetadatas =
			DLFileEntryMetadataLocalServiceUtil.
				getFileVersionFileEntryMetadatas(
					dlFileVersion.getFileVersionId());

		StringBundler sb = new StringBundler(dlFileEntryMetadatas.size());

		for (DLFileEntryMetadata dlFileEntryMetadata : dlFileEntryMetadatas) {
			DDMFormValues ddmFormValues = null;

			try {
				ddmFormValues = StorageEngineManagerUtil.getDDMFormValues(
					dlFileEntryMetadata.getDDMStorageId());
			}
			catch (Exception e) {
			}

			if (ddmFormValues != null) {
				sb.append(
					DDMStructureManagerUtil.extractAttributes(
						dlFileEntryMetadata.getDDMStructureId(), ddmFormValues,
						locale));
			}
		}

		return sb.toString();
	}

	protected void reindexFileEntries(
			long companyId, final long groupId, final long dataRepositoryId)
		throws PortalException {

		final IndexableActionableDynamicQuery indexableActionableDynamicQuery =
			DLFileEntryLocalServiceUtil.getIndexableActionableDynamicQuery();

		indexableActionableDynamicQuery.setAddCriteriaMethod(
			new ActionableDynamicQuery.AddCriteriaMethod() {

				@Override
				public void addCriteria(DynamicQuery dynamicQuery) {
					Property property = PropertyFactoryUtil.forName("folderId");

					long folderId = DLFolderConstants.getFolderId(
						groupId, dataRepositoryId);

					dynamicQuery.add(property.eq(folderId));
				}

			});
		indexableActionableDynamicQuery.setCompanyId(companyId);
		indexableActionableDynamicQuery.setGroupId(groupId);
		indexableActionableDynamicQuery.setInterval(
			PropsValues.DL_FILE_INDEXING_INTERVAL);
		indexableActionableDynamicQuery.setPerformActionMethod(
			new ActionableDynamicQuery.PerformActionMethod<DLFileEntry>() {

				@Override
				public void performAction(DLFileEntry dlFileEntry) {
					try {
						Document document = getDocument(dlFileEntry);

						indexableActionableDynamicQuery.addDocuments(document);
					}
					catch (PortalException pe) {
						if (_log.isWarnEnabled()) {
							_log.warn(
								"Unable to index document library file entry " +
									dlFileEntry.getFileEntryId(),
								pe);
						}
					}
				}

			});
		indexableActionableDynamicQuery.setSearchEngineId(getSearchEngineId());

		indexableActionableDynamicQuery.performActions();
	}

	protected void reindexFolders(final long companyId) throws PortalException {
		ActionableDynamicQuery actionableDynamicQuery =
			DLFolderLocalServiceUtil.getActionableDynamicQuery();

		actionableDynamicQuery.setCompanyId(companyId);
		actionableDynamicQuery.setPerformActionMethod(
			new ActionableDynamicQuery.PerformActionMethod<DLFolder>() {

				@Override
				public void performAction(DLFolder dlFolder)
					throws PortalException {

					long groupId = dlFolder.getGroupId();
					long folderId = dlFolder.getFolderId();

					String[] newIds = {
						String.valueOf(companyId), String.valueOf(groupId),
						String.valueOf(folderId)
					};

					reindex(newIds);
				}

			});

		actionableDynamicQuery.performActions();
	}

	protected void reindexRoot(final long companyId) throws PortalException {
		ActionableDynamicQuery actionableDynamicQuery =
			GroupLocalServiceUtil.getActionableDynamicQuery();

		actionableDynamicQuery.setCompanyId(companyId);
		actionableDynamicQuery.setPerformActionMethod(
			new ActionableDynamicQuery.PerformActionMethod<Group>() {

				@Override
				public void performAction(Group group) throws PortalException {
					long groupId = group.getGroupId();

					long folderId = groupId;

					String[] newIds = {
						String.valueOf(companyId), String.valueOf(groupId),
						String.valueOf(folderId)
					};

					reindex(newIds);
				}

			});

		actionableDynamicQuery.performActions();
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DLFileEntryIndexer.class);

	private final RelatedEntryIndexer _relatedEntryIndexer =
		new BaseRelatedEntryIndexer();

}
package de.metas.ui.web.view;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import de.metas.ui.web.exceptions.EntityNotFoundException;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.datatypes.DocumentPath;
import de.metas.ui.web.window.datatypes.WindowId;
import de.metas.ui.web.window.datatypes.json.JSONLookupValue;
import lombok.NonNull;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2016 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

public final class DocumentView implements IDocumentView
{
	public static final Builder builder(final WindowId windowId)
	{
		return new Builder(windowId);
	}

	private final DocumentPath documentPath;
	private final DocumentId documentId;
	private final IDocumentViewType type;
	private final boolean processed;

	private final Map<String, Object> values;

	private final IDocumentViewAttributesProvider attributesProvider;

	private final List<IDocumentView> includedDocuments;

	private DocumentView(final Builder builder)
	{
		super();
		documentPath = builder.getDocumentPath();

		documentId = documentPath.getDocumentId();
		type = builder.getType();
		processed = builder.isProcessed();

		values = ImmutableMap.copyOf(builder.values);

		includedDocuments = builder.buildIncludedDocuments();

		attributesProvider = builder.getAttributesProviderOrNull();
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.omitNullValues()
				.add("id", documentId)
				.add("type", type)
				.add("values", values)
				.add("attributesProvider", attributesProvider)
				.add("includedDocuments.count", includedDocuments.size())
				.add("processed", processed)
				.toString();
	}

	@Override
	public DocumentPath getDocumentPath()
	{
		return documentPath;
	}

	@Override
	public DocumentId getDocumentId()
	{
		return documentId;
	}

	@Override
	public IDocumentViewType getType()
	{
		return type;
	}

	@Override
	public boolean isProcessed()
	{
		return processed;
	}

	@Override
	public Map<String, Object> getFieldNameAndJsonValues()
	{
		return values;
	}

	@Override
	public boolean hasAttributes()
	{
		return attributesProvider != null;
	}

	@Override
	public IDocumentViewAttributes getAttributes()
	{
		if (documentId == null)
		{
			throw new EntityNotFoundException("Document does not support attributes");
		}
		if (attributesProvider == null)
		{
			throw new EntityNotFoundException("Document does not support attributes");
		}

		final IDocumentViewAttributes attributes = attributesProvider.getAttributes(documentId, documentId);
		if (attributes == null)
		{
			throw new EntityNotFoundException("Document does not support attributes");
		}
		return attributes;
	}

	@Override
	public boolean hasIncludedView()
	{
		return false;
	}

	@Override
	public List<IDocumentView> getIncludedDocuments()
	{
		return includedDocuments;
	}

	//
	//
	//
	//
	//
	public static final class Builder
	{
		private final WindowId windowId;
		private String idFieldName;
		private DocumentId _documentId;
		private IDocumentViewType type;
		private Boolean processed;

		private final Map<String, Object> values = new LinkedHashMap<>();

		private List<IDocumentView> includedDocuments = null;

		private IDocumentViewAttributesProvider attributesProvider;

		private Builder(@NonNull final WindowId windowId)
		{
			this.windowId = windowId;
		}

		public DocumentView build()
		{
			return new DocumentView(this);
		}

		private DocumentPath getDocumentPath()
		{
			final DocumentId documentId = getDocumentId();
			return DocumentPath.rootDocumentPath(windowId, documentId);
		}

		public Builder setIdFieldName(final String idFieldName)
		{
			this.idFieldName = idFieldName;
			return this;
		}

		public Builder setDocumentId(final DocumentId documentId)
		{
			_documentId = documentId;
			return this;
		}

		/** @return view row ID */
		private DocumentId getDocumentId()
		{
			if (_documentId != null)
			{
				return _documentId;
			}

			if (idFieldName == null)
			{
				throw new IllegalStateException("No idFieldName was specified");
			}

			final Object idJson = values.get(idFieldName);

			if (idJson == null)
			{
				throw new IllegalArgumentException("No ID found for " + idFieldName);
			}
			if (idJson instanceof Integer)
			{
				return DocumentId.of((Integer)idJson);
			}
			else if (idJson instanceof String)
			{
				return DocumentId.of(idJson.toString());
			}
			else if (idJson instanceof JSONLookupValue)
			{
				// case: usually this is happening when a view's column which is Lookup is also marked as KEY.
				final JSONLookupValue jsonLookupValue = (JSONLookupValue)idJson;
				return DocumentId.of(jsonLookupValue.getKey());
			}
			else
			{
				throw new IllegalArgumentException("Cannot convert id '" + idJson + "' (" + idJson.getClass() + ") to integer");
			}
		}

		private IDocumentViewType getType()
		{
			return type;
		}

		public Builder setType(final IDocumentViewType type)
		{
			this.type = type;
			return this;
		}

		public Builder setProcessed(final boolean processed)
		{
			this.processed = processed;
			return this;
		}

		private boolean isProcessed()
		{
			if (processed == null)
			{
				// NOTE: don't take the "Processed" field if any, because in frontend we will end up with a lot of grayed out completed sales orders, for example.
				// return DisplayType.toBoolean(values.getOrDefault("Processed", false));
				return false;
			}
			else
			{
				return processed.booleanValue();
			}
		}

		public Builder putFieldValue(final String fieldName, final Object jsonValue)
		{
			if (jsonValue == null)
			{
				values.remove(fieldName);
			}
			else
			{
				values.put(fieldName, jsonValue);
			}

			return this;
		}

		private IDocumentViewAttributesProvider getAttributesProviderOrNull()
		{
			return attributesProvider;
		}

		public Builder setAttributesProvider(@Nullable final IDocumentViewAttributesProvider attributesProvider)
		{
			this.attributesProvider = attributesProvider;
			return this;
		}

		public Builder addIncludedDocument(final IDocumentView includedDocument)
		{
			if (includedDocuments == null)
			{
				includedDocuments = new ArrayList<>();
			}

			includedDocuments.add(includedDocument);

			return this;
		}

		private List<IDocumentView> buildIncludedDocuments()
		{
			if (includedDocuments == null || includedDocuments.isEmpty())
			{
				return ImmutableList.of();
			}

			return ImmutableList.copyOf(includedDocuments);
		}

	}
}
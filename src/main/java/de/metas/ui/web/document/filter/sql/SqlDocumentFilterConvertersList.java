package de.metas.ui.web.document.filter.sql;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableMap;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2017 metas GmbH
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

/**
 * Immutable collection of {@link SqlDocumentFilterConverter}s indexed by filterId.
 *
 * To create new instances, please use {@link SqlDocumentFilterConverters}.
 *
 * @author metas-dev <dev@metasfresh.com>
 *
 */
@Immutable
@ToString
@EqualsAndHashCode
public final class SqlDocumentFilterConvertersList
{
	/* package */static final Builder builder()
	{
		return new Builder();
	}

	/* package */static final SqlDocumentFilterConvertersList EMPTY = new SqlDocumentFilterConvertersList(ImmutableMap.of());

	private final ImmutableMap<String, SqlDocumentFilterConverter> convertersByFilterId;

	private SqlDocumentFilterConvertersList(final ImmutableMap<String, SqlDocumentFilterConverter> convertersByFilterId)
	{
		this.convertersByFilterId = convertersByFilterId;
	}

	public SqlDocumentFilterConverter getConverterOrDefault(final String filterId, final SqlDocumentFilterConverter defaultConverter)
	{
		return convertersByFilterId.getOrDefault(filterId, defaultConverter);
	}

	//
	//
	//
	//
	//
	public static class Builder
	{
		private ImmutableMap.Builder<String, SqlDocumentFilterConverter> convertersByFilterId = null;

		private Builder()
		{
		}

		public SqlDocumentFilterConvertersList build()
		{
			if (convertersByFilterId == null)
			{
				return EMPTY;
			}

			final ImmutableMap<String, SqlDocumentFilterConverter> convertersByFilterId = this.convertersByFilterId.build();
			if (convertersByFilterId.isEmpty())
			{
				return EMPTY;
			}

			return new SqlDocumentFilterConvertersList(convertersByFilterId);
		}

		public Builder addConverter(final String filterId, final SqlDocumentFilterConverter converter)
		{
			if (convertersByFilterId == null)
			{
				convertersByFilterId = ImmutableMap.builder();
			}
			convertersByFilterId.put(filterId, converter);
			return this;
		}
	}
}
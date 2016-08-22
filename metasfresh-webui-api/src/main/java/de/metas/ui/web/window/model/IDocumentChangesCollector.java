package de.metas.ui.web.window.model;

import java.util.Map;
import java.util.Set;

import de.metas.ui.web.window.datatypes.DocumentPath;

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

public interface IDocumentChangesCollector
{
	Set<String> getFieldNames(DocumentPath documentPath);

	boolean isEmpty();

//	List<DocumentFieldChangedEvent> toEventsList();
	Map<DocumentPath, DocumentChanges> getDocumentChangesByPath();

	void collectValueChanged(IDocumentFieldView documentField, ReasonSupplier reason);

	void collectReadonlyChanged(IDocumentFieldView documentField, ReasonSupplier reason);

	void collectMandatoryChanged(IDocumentFieldView documentField, ReasonSupplier reason);

	void collectDisplayedChanged(IDocumentFieldView documentField, ReasonSupplier reason);

	void collectLookupValuesStaled(IDocumentFieldView documentField, ReasonSupplier reason);

	void collectFrom(IDocumentChangesCollector fromCollector);

	/**
	 * Collect changes from given document (only those which were not yet collected).
	 * 
	 * @param fromCollector
	 * @return true if something was collected
	 */
	boolean collectFrom(Document document, ReasonSupplier reason);

	@FunctionalInterface
	interface ReasonSupplier
	{
		/**
		 * @return actual reason string
		 */
		String get();
	}
}
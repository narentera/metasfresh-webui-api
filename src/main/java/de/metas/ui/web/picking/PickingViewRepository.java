package de.metas.ui.web.picking;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.util.Services;
import org.compiere.util.DisplayType;
import org.springframework.stereotype.Component;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import de.metas.inoutcandidate.model.I_M_Packageable_V;
import de.metas.ui.web.view.ViewId;
import de.metas.ui.web.view.ViewRow.DefaultRowType;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.datatypes.DocumentPath;
import de.metas.ui.web.window.descriptor.DocumentFieldWidgetType;
import de.metas.ui.web.window.descriptor.LookupDescriptorProvider.LookupScope;
import de.metas.ui.web.window.descriptor.sql.SqlLookupDescriptor;
import de.metas.ui.web.window.model.lookup.LookupDataSource;
import de.metas.ui.web.window.model.lookup.LookupDataSourceFactory;

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

@Component
public class PickingViewRepository
{
	private final Supplier<LookupDataSource> warehouseLookup;
	private final Supplier<LookupDataSource> productLookup;

	public PickingViewRepository()
	{
		// creating those LookupDataSources requires DB access. so to allow this component to be initialized early during startup 
		// and also to allow it to be unit-tested (when the lookups are not part of the test), I use those suppliers

		warehouseLookup = Suppliers.memoize(() -> LookupDataSourceFactory.instance.getLookupDataSource(SqlLookupDescriptor.builder()
				.setColumnName(I_M_Packageable_V.COLUMNNAME_M_Warehouse_ID)
				.setDisplayType(DisplayType.Search)
				.setWidgetType(DocumentFieldWidgetType.Lookup)
				.buildProvider()
				.provideForScope(LookupScope.DocumentField)));

		productLookup = Suppliers.memoize(() -> LookupDataSourceFactory.instance.getLookupDataSource(SqlLookupDescriptor.builder()
				.setColumnName(I_M_Packageable_V.COLUMNNAME_M_Product_ID)
				.setDisplayType(DisplayType.Search)
				.setWidgetType(DocumentFieldWidgetType.Lookup)
				.buildProvider()
				.provideForScope(LookupScope.DocumentField)));
	}

	public List<PickingRow> retrieveRowsByIds(final ViewId viewId, final Collection<DocumentId> rowIds)
	{
		final Set<Integer> shipmentScheduleIds = rowIds.stream().map(DocumentId::toInt).collect(ImmutableSet.toImmutableSet());
		if (shipmentScheduleIds.isEmpty())
		{
			return ImmutableList.of();
		}

		return Services.get(IQueryBL.class)
				.createQueryBuilder(I_M_Packageable_V.class)
				.addInArrayFilter(I_M_Packageable_V.COLUMN_M_ShipmentSchedule_ID, shipmentScheduleIds)
				.create()
				.stream(I_M_Packageable_V.class)
				.map(packageable -> createPickingRow(viewId, packageable))
				.collect(ImmutableList.toImmutableList());
	}

	private PickingRow createPickingRow(final ViewId viewId, final I_M_Packageable_V packageable)
	{
		final DocumentId rowId = DocumentId.of(packageable.getM_ShipmentSchedule_ID());
		final DocumentPath documentPath = DocumentPath.rootDocumentPath(PickingConstants.WINDOWID_PickingView, rowId);
		return PickingRow.builder()
				.documentPath(documentPath)
				.viewId(viewId)
				.id(rowId)
				.type(DefaultRowType.Row)
				.processed(false)
				//
				.warehouse(warehouseLookup.get().findById(packageable.getM_Warehouse_ID()))
				.product(productLookup.get().findById(packageable.getM_Product_ID()))
				.deliveryDate(packageable.getDeliveryDate())
				.preparationDate(packageable.getPreparationDate())
				.qtyToDeliver(packageable.getQtyToDeliver())
				//
				.build();
	}
}

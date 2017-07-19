package de.metas.ui.web.picking;

import java.util.List;
import java.util.Map;

import org.adempiere.ad.dao.ICompositeQueryUpdater;
import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.dao.IQueryBuilder;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.GuavaCollectors;
import org.adempiere.util.Services;
import org.adempiere.util.lang.IPair;
import org.adempiere.util.lang.ImmutablePair;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;

import de.metas.picking.model.I_M_PickingSlot;
import de.metas.picking.model.I_M_Picking_Candidate;
import de.metas.printing.esb.base.util.Check;
import de.metas.ui.web.handlingunits.HUEditorRow;
import de.metas.ui.web.handlingunits.HUEditorRowAttributesProvider;
import de.metas.ui.web.handlingunits.HUEditorViewRepository;
import lombok.NonNull;

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
/* package */class PickingHUsRepository
{
	private final HUEditorViewRepository huEditorRepo;

	public PickingHUsRepository()
	{
		this(HUEditorViewRepository.builder()
				.windowId(PickingConstants.WINDOWID_PickingSlotView)
				.referencingTableName(I_M_PickingSlot.Table_Name)
				.attributesProvider(HUEditorRowAttributesProvider.builder().readonly(true).build())
				.build());
	}

	@VisibleForTesting
	/* package */ PickingHUsRepository(final HUEditorViewRepository huEditorRepo)
	{
		this.huEditorRepo = huEditorRepo;
	}

	/**
	 * 
	 * @param shipmentScheduleId
	 * @return a multi-map where the keys are {@code M_PickingSlot_ID}s and the value is a list of HUEditorRows with the respective picking candidates' processed status values.
	 */
	public ListMultimap<Integer, PickingSlotHUEditorRow> retrieveHUsIndexedByPickingSlotId(@NonNull final PickingSlotRepoQuery pickingSlotRowQuery)
	{
		// configure the query builder
		final IQueryBL queryBL = Services.get(IQueryBL.class);

		final IQueryBuilder<I_M_Picking_Candidate> queryBuilder = queryBL
				.createQueryBuilder(I_M_Picking_Candidate.class)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_M_Picking_Candidate.COLUMN_M_ShipmentSchedule_ID, pickingSlotRowQuery.getShipmentScheduleId());

		switch (pickingSlotRowQuery.getPickingCandidates())
		{
			case DONT_CARE:
				break;
			case ONLY_PROCESSED:
				queryBuilder.addEqualsFilter(I_M_Picking_Candidate.COLUMN_Processed, true);
				break;
			case ONLY_UNPROCESSED:
				queryBuilder.addEqualsFilter(I_M_Picking_Candidate.COLUMN_Processed, false);
				break;
			default:
				Check.errorIf(true, "Query has unexpected pickingCandidates={}; query={}", pickingSlotRowQuery.getPickingCandidates(), pickingSlotRowQuery);
		}

		// execute the query and process the result
		final Map<Integer, IPair<Integer, Boolean>> huId2pickingSlotId = queryBuilder
				.create()
				.stream(I_M_Picking_Candidate.class)
				.collect(ImmutableMap.toImmutableMap(
						I_M_Picking_Candidate::getM_HU_ID, // key function
						pc -> ImmutablePair.of(pc.getM_PickingSlot_ID(), pc.isProcessed())) // value function
		);

		final List<HUEditorRow> huRows = huEditorRepo.retrieveHUEditorRows(
				huId2pickingSlotId.keySet());

		final ListMultimap<Integer, PickingSlotHUEditorRow> result = huRows.stream()
				.map(huRow -> GuavaCollectors.entry(
						huId2pickingSlotId.get(huRow.getM_HU_ID()).getLeft(), // the results key, i.e. M_PickingSlot_ID
						new PickingSlotHUEditorRow(// the result's values
								huRow, // the actual row
								huId2pickingSlotId.get(huRow.getM_HU_ID()).getRight()) // M_Picking_Candidate.Processed
				))
				.collect(GuavaCollectors.toImmutableListMultimap());

		return result;
	}

	/**
	 * Immutable pojo that contains the HU editor as retrieved from {@link HUEditorViewRepository} plus the the {@code processed} value from the respective {@link I_M_Picking_Candidate}.
	 * 
	 * @author metas-dev <dev@metasfresh.com>
	 *
	 */
	// the fully qualified annotations are a workaround for a javac problem with maven
	@lombok.Data
	@lombok.AllArgsConstructor
	public static class PickingSlotHUEditorRow
	{
		private final HUEditorRow huEditor;

		private final boolean processed;
	}

	public void addHUToPickingSlot(final int huId, final int pickingSlotId, final int shipmentScheduleId)
	{
		final I_M_Picking_Candidate pickingCandidatePO = InterfaceWrapperHelper.newInstance(I_M_Picking_Candidate.class);
		pickingCandidatePO.setM_ShipmentSchedule_ID(shipmentScheduleId);
		pickingCandidatePO.setM_PickingSlot_ID(pickingSlotId);
		pickingCandidatePO.setM_HU_ID(huId);
		InterfaceWrapperHelper.save(pickingCandidatePO);
	}

	public I_M_Picking_Candidate getCreateCandidate(final int huId, final int pickingSlotId, final int shipmentScheduleId)
	{
		I_M_Picking_Candidate pickingCandidatePO = Services.get(IQueryBL.class)
				.createQueryBuilder(I_M_Picking_Candidate.class)
				.addEqualsFilter(I_M_Picking_Candidate.COLUMN_M_PickingSlot_ID, pickingSlotId)
				.addEqualsFilter(I_M_Picking_Candidate.COLUMNNAME_M_HU_ID, huId)
				.addEqualsFilter(I_M_Picking_Candidate.COLUMNNAME_M_ShipmentSchedule_ID, shipmentScheduleId)
				.create()
				.firstOnly(I_M_Picking_Candidate.class);
		if (pickingCandidatePO == null)
		{
			pickingCandidatePO = InterfaceWrapperHelper.newInstance(I_M_Picking_Candidate.class);
			pickingCandidatePO.setM_ShipmentSchedule_ID(shipmentScheduleId);
			pickingCandidatePO.setM_PickingSlot_ID(pickingSlotId);
			pickingCandidatePO.setM_HU_ID(huId);
			InterfaceWrapperHelper.save(pickingCandidatePO);
		}

		return pickingCandidatePO;
	}

	public void removeHUFromPickingSlot(final int huId, final int pickingSlotId)
	{
		Services.get(IQueryBL.class)
				.createQueryBuilder(I_M_Picking_Candidate.class)
				.addEqualsFilter(I_M_Picking_Candidate.COLUMN_M_PickingSlot_ID, pickingSlotId)
				.addEqualsFilter(I_M_Picking_Candidate.COLUMNNAME_M_HU_ID, huId)
				.create()
				.delete();
	}

	public void setCandidatesProcessed(@NonNull final List<Integer> huIds)
	{
		if (huIds.isEmpty())
		{
			return;
		}
		final IQueryBL queryBL = Services.get(IQueryBL.class);

		final ICompositeQueryUpdater<I_M_Picking_Candidate> updater = queryBL.createCompositeQueryUpdater(I_M_Picking_Candidate.class)
				.addSetColumnValue(I_M_Picking_Candidate.COLUMNNAME_Processed, true);

		queryBL.createQueryBuilder(I_M_Picking_Candidate.class)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_M_Picking_Candidate.COLUMNNAME_Processed, false)
				.addInArrayFilter(I_M_Picking_Candidate.COLUMNNAME_M_HU_ID, huIds)
				.create()
				.updateDirectly(updater);
	}
}

package de.metas.ui.web.quickinput.orderline;

import static org.adempiere.model.InterfaceWrapperHelper.load;

import java.math.BigDecimal;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.mm.attributes.api.IAttributeSetInstanceBL;
import org.adempiere.mm.attributes.api.ImmutableAttributeSet;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Services;
import org.compiere.model.I_C_OrderLine;
import org.compiere.model.I_M_AttributeSetInstance;
import org.compiere.model.I_M_Product;
import org.slf4j.Logger;

import de.metas.adempiere.callout.OrderFastInput;
import de.metas.adempiere.gui.search.HUPackingAwareCopy.ASICopyMode;
import de.metas.adempiere.gui.search.IHUPackingAware;
import de.metas.adempiere.gui.search.IHUPackingAwareBL;
import de.metas.adempiere.gui.search.impl.OrderLineHUPackingAware;
import de.metas.adempiere.gui.search.impl.PlainHUPackingAware;
import de.metas.adempiere.model.I_C_Order;
import de.metas.handlingunits.model.I_M_HU_PI_Item_Product;
import de.metas.logging.LogManager;
import de.metas.ui.web.quickinput.IQuickInputProcessor;
import de.metas.ui.web.quickinput.QuickInput;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.descriptor.sql.ProductLookupDescriptor;
import de.metas.ui.web.window.descriptor.sql.ProductLookupDescriptor.ProductAndAttributes;
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

public class OrderLineQuickInputProcessor implements IQuickInputProcessor
{
	// services
	private static final transient Logger logger = LogManager.getLogger(OrderLineQuickInputProcessor.class);
	private final transient IHUPackingAwareBL huPackingAwareBL = Services.get(IHUPackingAwareBL.class);

	public OrderLineQuickInputProcessor()
	{
		super();
	}

	@Override
	public DocumentId process(final QuickInput quickInput)
	{
		final I_C_Order order = quickInput.getRootDocumentAs(I_C_Order.class);
		final Properties ctx = InterfaceWrapperHelper.getCtx(order);

		final I_C_OrderLine newOrderLine = OrderFastInput.addOrderLine(ctx, order, orderLineObj -> updateOrderLine(orderLineObj, quickInput));
		final int newOrderLineId = newOrderLine.getC_OrderLine_ID();
		return DocumentId.of(newOrderLineId);
	}

	private final void updateOrderLine(final Object orderLineObj, final QuickInput fromQuickInput)
	{
		final I_C_Order order = fromQuickInput.getRootDocumentAs(I_C_Order.class);
		final IOrderLineQuickInput fromOrderLineQuickInput = fromQuickInput.getQuickInputDocumentAs(IOrderLineQuickInput.class);
		final IHUPackingAware quickInputPackingAware = createQuickInputPackingAware(order, fromOrderLineQuickInput);

		final I_C_OrderLine orderLineToUpdate = InterfaceWrapperHelper.create(orderLineObj, I_C_OrderLine.class);
		final IHUPackingAware orderLinePackingAware = OrderLineHUPackingAware.of(orderLineToUpdate);

		huPackingAwareBL.prepareCopyFrom(quickInputPackingAware)
				.overridePartner(false)
				.asiCopyMode(ASICopyMode.CopyID) // because we just created the ASI
				.copyTo(orderLinePackingAware);
	}

	private IHUPackingAware createQuickInputPackingAware(
			@NonNull final I_C_Order order,
			@NonNull final IOrderLineQuickInput quickInput)
	{
		final PlainHUPackingAware huPackingAware = createAndInitHuPackingAware(order, quickInput);

		// Get quickInput's Qty
		final BigDecimal quickInputQty = quickInput.getQty();
		if (quickInputQty == null || quickInputQty.signum() <= 0)
		{
			logger.warn("Invalid Qty={} for {}", quickInputQty, quickInput);
			throw new AdempiereException("Qty shall be greather than zero"); // TODO trl
		}

		huPackingAwareBL.computeAndSetQtysForNewHuPackingAware(huPackingAware, quickInputQty);

		return validateNewHuPackingAware(huPackingAware);
	}

	private PlainHUPackingAware createAndInitHuPackingAware(
			@NonNull final I_C_Order order,
			@NonNull final IOrderLineQuickInput quickInput)
	{
		final PlainHUPackingAware huPackingAware = new PlainHUPackingAware();
		huPackingAware.setC_BPartner(order.getC_BPartner());
		huPackingAware.setDateOrdered(order.getDateOrdered());
		huPackingAware.setInDispute(false);

		final ProductAndAttributes productAndAttributes = ProductLookupDescriptor.toProductAndAttributes(quickInput.getM_Product_ID());
		final I_M_Product product = load(productAndAttributes.getProductId(), I_M_Product.class);
		huPackingAware.setM_Product_ID(product.getM_Product_ID());
		huPackingAware.setC_UOM(product.getC_UOM());
		huPackingAware.setM_AttributeSetInstance_ID(createASI(productAndAttributes));

		final I_M_HU_PI_Item_Product piItemProduct = quickInput.getM_HU_PI_Item_Product();
		huPackingAware.setM_HU_PI_Item_Product(piItemProduct);

		return huPackingAware;
	}

	private PlainHUPackingAware validateNewHuPackingAware(@NonNull final PlainHUPackingAware huPackingAware)
	{
		if (huPackingAware.getQty() == null || huPackingAware.getQty().signum() <= 0)
		{
			logger.warn("Invalid Qty={} for {}", huPackingAware.getQty(), huPackingAware);
			throw new AdempiereException("Qty shall be greather than zero"); // TODO trl
		}
		if (huPackingAware.getQtyTU() == null || huPackingAware.getQtyTU().signum() <= 0)
		{
			logger.warn("Invalid QtyTU={} for {}", huPackingAware.getQtyTU(), huPackingAware);
			throw new AdempiereException("QtyTU shall be greather than zero"); // TODO trl
		}
		return huPackingAware;
	}

	private static final int createASI(final ProductAndAttributes productAndAttributes)
	{
		final ImmutableAttributeSet attributes = productAndAttributes.getAttributes();
		if (attributes.isEmpty())
		{
			return -1;
		}

		final IAttributeSetInstanceBL asiBL = Services.get(IAttributeSetInstanceBL.class);

		final I_M_AttributeSetInstance asi = asiBL.createASIWithASFromProductAndInsertAttributeSet(
				productAndAttributes.getProductId(),
				attributes);

		return asi.getM_AttributeSetInstance_ID();
	}
}

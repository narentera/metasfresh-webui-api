package de.metas.ui.web.order.sales.pricingConditions.view;

import java.math.BigDecimal;
import java.util.Optional;

import de.metas.lang.Percent;
import de.metas.payment.api.PaymentTermId;
import de.metas.pricing.PricingSystemId;
import de.metas.pricing.conditions.PriceOverride;
import de.metas.pricing.conditions.PriceOverrideType;
import de.metas.pricing.conditions.PricingConditionsBreak;
import de.metas.pricing.conditions.PricingConditionsBreakId;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2018 metas GmbH
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

@Value
@Builder
public class PricingConditionsRowChangeRequest
{
	PricingConditionsBreak pricingConditionsBreak;

	PricingConditionsBreakId sourcePricingConditionsBreakId;

	Percent discount;
	Optional<PaymentTermId> paymentTermId;

	PriceChange priceChange;

	public static interface PriceChange
	{
	}

	@lombok.Value
	@lombok.Builder
	public static final class PartialPriceChange implements PriceChange
	{
		PriceOverrideType priceType;
		Optional<PricingSystemId> basePricingSystemId;
		BigDecimal basePriceAddAmt;
		BigDecimal fixedPrice;
	}

	@lombok.Value(staticConstructor = "of")
	public static final class CompletePriceChange implements PriceChange
	{
		@NonNull
		PriceOverride price;
	}
}

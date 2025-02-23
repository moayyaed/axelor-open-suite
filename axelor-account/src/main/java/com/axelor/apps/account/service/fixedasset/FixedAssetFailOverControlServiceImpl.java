/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.apps.account.service.fixedasset;

import com.axelor.apps.account.db.FixedAsset;
import com.axelor.apps.account.db.FixedAssetCategory;
import com.axelor.apps.account.db.repo.FixedAssetRepository;
import com.axelor.apps.account.exception.AccountExceptionMessage;
import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.Objects;

public class FixedAssetFailOverControlServiceImpl implements FixedAssetFailOverControlService {

  protected FixedAssetLineToolService fixedAssetLineToolService;

  @Inject
  public FixedAssetFailOverControlServiceImpl(FixedAssetLineToolService fixedAssetLineToolService) {
    this.fixedAssetLineToolService = fixedAssetLineToolService;
  }

  /**
   * {@inheritDoc}
   *
   * @throws AxelorException
   */
  @Override
  public void controlFailOver(FixedAsset fixedAsset) throws AxelorException {
    Objects.requireNonNull(fixedAsset);
    FixedAssetCategory fixedAssetCategory = fixedAsset.getFixedAssetCategory();
    BigDecimal grossValue = fixedAsset.getGrossValue();
    BigDecimal importAlreadyDepreciatedAmount = fixedAsset.getImportAlreadyDepreciatedAmount();
    BigDecimal importFiscalAlreadyDepreciationAmount =
        fixedAsset.getImportFiscalAlreadyDepreciatedAmount();
    BigDecimal importIfrsAlreadyDepreciatedAmount =
        fixedAsset.getImportIfrsAlreadyDepreciatedAmount();

    if (isFailOver(fixedAsset) && fixedAssetCategory != null) {
      if (fixedAssetLineToolService.isGreaterThan(
          importAlreadyDepreciatedAmount, grossValue, fixedAsset)) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_INCONSISTENCY,
            I18n.get(
                AccountExceptionMessage
                    .IMMO_FIXED_ASSET_FAILOVER_CONTROL_PAST_DEPRECIATION_GREATER_THAN_GROSS_VALUE));
      }
      if (fixedAssetLineToolService.isGreaterThan(
          importFiscalAlreadyDepreciationAmount, grossValue, fixedAsset)) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_INCONSISTENCY,
            I18n.get(
                AccountExceptionMessage
                    .IMMO_FIXED_ASSET_FAILOVER_CONTROL_PAST_DEPRECIATION_GREATER_THAN_GROSS_VALUE));
      }
      if (fixedAssetLineToolService.isGreaterThan(
          importIfrsAlreadyDepreciatedAmount, grossValue, fixedAsset)) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_INCONSISTENCY,
            I18n.get(
                AccountExceptionMessage
                    .IMMO_FIXED_ASSET_FAILOVER_CONTROL_PAST_DEPRECIATION_GREATER_THAN_GROSS_VALUE));
      }
    }
  }

  @Override
  public boolean isFailOver(FixedAsset fixedAsset) {
    return fixedAsset.getOriginSelect() == FixedAssetRepository.ORIGINAL_SELECT_IMPORT
        && fixedAsset.getImportDepreciationDate() != null;
  }
}

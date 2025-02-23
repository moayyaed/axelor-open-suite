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
package com.axelor.apps.bankpayment.service.invoice.payment;

import com.axelor.apps.account.db.InvoicePayment;
import com.axelor.apps.account.db.repo.InvoicePaymentRepository;
import com.axelor.apps.account.exception.AccountExceptionMessage;
import com.axelor.apps.account.service.invoice.InvoiceTermService;
import com.axelor.apps.account.service.move.MoveCancelService;
import com.axelor.apps.account.service.payment.invoice.payment.InvoicePaymentCancelServiceImpl;
import com.axelor.apps.account.service.payment.invoice.payment.InvoicePaymentToolService;
import com.axelor.apps.bankpayment.db.BankOrder;
import com.axelor.apps.bankpayment.db.repo.BankOrderRepository;
import com.axelor.apps.bankpayment.service.app.AppBankPaymentService;
import com.axelor.apps.bankpayment.service.bankorder.BankOrderCancelService;
import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvoicePaymentCancelServiceBankPayImpl extends InvoicePaymentCancelServiceImpl
    implements InvoicePaymentBankPaymentCancelService {

  protected BankOrderCancelService bankOrderCancelService;

  private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected AppBankPaymentService appBankPaymentService;

  @Inject
  public InvoicePaymentCancelServiceBankPayImpl(
      InvoicePaymentRepository invoicePaymentRepository,
      MoveCancelService moveCancelService,
      InvoicePaymentToolService invoicePaymentToolService,
      InvoiceTermService invoiceTermService,
      BankOrderCancelService bankOrderCancelService,
      AppBankPaymentService appBankPaymentService) {
    super(
        invoicePaymentRepository, moveCancelService, invoicePaymentToolService, invoiceTermService);
    this.bankOrderCancelService = bankOrderCancelService;
    this.appBankPaymentService = appBankPaymentService;
  }

  /**
   * Method to cancel an invoice Payment
   *
   * <p>Cancel the eventual Move and Reconcile Compute the total amount paid on the linked invoice
   * Change the status to cancel
   *
   * @param invoicePayment An invoice payment
   * @throws AxelorException
   */
  @Override
  @Transactional(rollbackOn = {Exception.class})
  public void cancel(InvoicePayment invoicePayment) throws AxelorException {

    if (!appBankPaymentService.isApp("bank-payment")) {
      super.cancel(invoicePayment);
      return;
    }

    this.checkPaymentBankOrder(invoicePayment);

    BankOrder paymentBankOrder = invoicePayment.getBankOrder();
    if (paymentBankOrder != null
        && paymentBankOrder.getStatusSelect() != BankOrderRepository.STATUS_CANCELED) {
      bankOrderCancelService.cancelBankOrder(paymentBankOrder);
      this.updateCancelStatus(invoicePayment);
    }

    super.cancel(invoicePayment);
  }

  @Override
  @Transactional(rollbackOn = {Exception.class})
  public void cancelInvoicePayment(InvoicePayment invoicePayment) throws AxelorException {
    this.checkPaymentBankOrder(invoicePayment);
    super.cancel(invoicePayment);
  }

  protected void checkPaymentBankOrder(InvoicePayment invoicePayment) throws AxelorException {
    BankOrder paymentBankOrder = invoicePayment.getBankOrder();

    if (paymentBankOrder != null
        && (paymentBankOrder.getStatusSelect() == BankOrderRepository.STATUS_CARRIED_OUT)) {
      throw new AxelorException(
          invoicePayment,
          TraceBackRepository.CATEGORY_INCONSISTENCY,
          I18n.get(AccountExceptionMessage.INVOICE_PAYMENT_CANCEL));
    }
  }
}

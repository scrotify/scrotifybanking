package com.scrotifybanking.scrotifybanking.service.impl;

import com.scrotifybanking.scrotifybanking.dto.response.ApiResponse;
import com.scrotifybanking.scrotifybanking.entity.Account;
import com.scrotifybanking.scrotifybanking.entity.Transaction;
import com.scrotifybanking.scrotifybanking.repository.AccountRepository;
import com.scrotifybanking.scrotifybanking.repository.TransactionRepository;
import com.scrotifybanking.scrotifybanking.service.TransactionService;
import com.scrotifybanking.scrotifybanking.util.ScrotifyConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * The type Transaction service.
 */
@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;


    /**
     * check balance before withdraw
     *
     * @param custId
     * @param amount
     * @param accountStatus
     * @param accountType
     * @return
     */
    @Override
    public boolean checkMinimumBalance(String custId, String accountStatus, String accountType, double amount) {
        double existingAmount = accountRepository.findByAccountBalance(custId, accountStatus, accountType);
        if (existingAmount > amount) {
            return true;
        }
        return false;
    }

    /**
     * Check maintenance balance after withdraw
     *
     * @param custId
     * @param amount
     * @param accountStatus
     * @param accountType
     * @return
     */
    @Override
    public boolean checkManintenanceBalance(String custId, String accountStatus, String accountType, double amount, double maintainBalance) {
        double existingAmount = accountRepository.findByAccountBalance(custId, accountStatus, accountType);
        if (existingAmount > (maintainBalance + amount)) {
            return true;
        }
        return false;
    }


    @Override
    public ApiResponse transferFund(String custId, String toAccountNo, double amount, String accountStatus, String accountType) {
        ApiResponse response = new ApiResponse();
        Account payeeAccount = null;
        Optional<Account> accountOptional = accountRepository.findById(Long.parseLong(toAccountNo));
        Account customerAccount = accountRepository.findByCustomerByAccount(custId, accountStatus, accountType);
        Optional<Account> customerAccountOptional = Optional.of(customerAccount);

        if (accountOptional.isPresent() && customerAccountOptional.isPresent()) {
            payeeAccount = accountOptional.get();
            double balanceAmount = customerAccount.getAvailableBalance() - amount;
            customerAccount.setAvailableBalance(balanceAmount);


            double payeeAccountBalance = payeeAccount.getAvailableBalance();
            payeeAccountBalance = payeeAccountBalance + amount;
            payeeAccount.setAvailableBalance(payeeAccountBalance);

            Transaction customertransaction = new Transaction();
            customertransaction.setAccount(customerAccount);
            customertransaction.setAmount(amount);
            customertransaction.setTransactionDate(LocalDate.now());
            customertransaction.setTransactionType(ScrotifyConstant.DEBIT_TRANSACTION);
            customertransaction.setPayeeNo(Long.parseLong(toAccountNo));

            Transaction payeeTransaction = new Transaction();
            payeeTransaction.setAccount(payeeAccount);
            payeeTransaction.setAmount(amount);
            payeeTransaction.setTransactionDate(LocalDate.now());
            payeeTransaction.setTransactionType(ScrotifyConstant.CREDIT_TRANSACTION);
            payeeTransaction.setPayeeNo(customertransaction.getAccount().getAccountNo());

            transactionRepository.save(customertransaction);
            transactionRepository.save(payeeTransaction);
            accountRepository.save(payeeAccount);
            accountRepository.save(customerAccount);
            response.setStatusCode(ScrotifyConstant.SUCCESS_CODE);
        }
        return response;
    }
}

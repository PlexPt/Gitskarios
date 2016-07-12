package com.alorma.github.ui.activity.login;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.os.Bundle;
import com.alorma.github.AccountsHelper;
import com.alorma.github.R;
import com.alorma.github.sdk.bean.dto.response.User;
import com.alorma.github.sdk.services.user.GetAuthUserClient;
import com.alorma.gitskarios.core.Pair;
import com.alorma.gitskarios.core.client.UrlProvider;
import java.lang.ref.WeakReference;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

class EnterpriseLoginPresenter {

  private EnterpriseLoginPresenterViewInterface nullView = new EnterpriseLoginPresenterViewInterface.NullView();
  private EnterpriseLoginPresenterViewInterface view = nullView;

  private WeakReference<AccountAuthenticatorActivity> accountAuthenticatorActivity;
  private String domainUrl;

  EnterpriseLoginPresenter(AccountAuthenticatorActivity accountAuthenticatorActivity) {
    this.accountAuthenticatorActivity = new WeakReference<>(accountAuthenticatorActivity);
  }

  public void login(String token, String domainUrl) {
    this.domainUrl = domainUrl;
    UrlProvider.setUrlProviderInstance(() -> domainUrl);
    Observable<Pair<User, String>> observable = new GetAuthUserClient(token).observable();
    observable.subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe(() -> view.willLogin())
        .doOnError(throwable -> view.didLogin())
        .doOnCompleted(() -> view.didLogin())
        .subscribe(new UserSubscription());
  }

  private class UserSubscription extends rx.Subscriber<Pair<User, String>> {
    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {
      view.onGenericError();
    }

    @Override
    public void onNext(Pair<User, String> userStringPair) {
      addAccount(userStringPair.first, userStringPair.second);
    }
  }

  public void start(EnterpriseLoginPresenterViewInterface loginPresenterViewInterface) {
    this.view = loginPresenterViewInterface;
  }

  public void stop() {
    this.view = nullView;
  }

  private void addAccount(User user, String accessToken) {
    if (accessToken != null && accountAuthenticatorActivity != null && accountAuthenticatorActivity.get() != null) {
      Account account = new Account(user.login, accountAuthenticatorActivity.get().getString(R.string.account_type));
      Bundle userData = AccountsHelper.buildBundle(user.name, user.email, user.avatar_url, domainUrl);
      userData.putString(AccountManager.KEY_AUTHTOKEN, accessToken);

      AccountManager accountManager = AccountManager.get(accountAuthenticatorActivity.get());
      accountManager.addAccountExplicitly(account, null, userData);
      accountManager.setAuthToken(account, accountAuthenticatorActivity.get().getString(R.string.account_type), accessToken);

      Bundle result = new Bundle();
      result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
      result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
      result.putString(AccountManager.KEY_AUTHTOKEN, accessToken);
      accountAuthenticatorActivity.get().setAccountAuthenticatorResult(result);
      view.finishAccess(user);
    }
  }
}

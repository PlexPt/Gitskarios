package com.alorma.github.ui.activity.login;

import android.accounts.AccountAuthenticatorActivity;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.alorma.github.R;
import com.alorma.github.sdk.bean.dto.response.User;
import com.alorma.github.ui.activity.MainActivity;
import com.alorma.github.utils.KeyboardUtils;

public class EnterpiseLoginActivity extends AccountAuthenticatorActivity implements EnterpriseLoginPresenterViewInterface {

  @Bind(R.id.toolbar) Toolbar toolbar;
  @Bind(R.id.openLogin) View buttonLogin;
  @Bind(R.id.login_url) TextInputLayout loginUrl;
  @Bind(R.id.login_token) TextInputLayout loginToken;
  private EnterpriseLoginPresenter presenter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login_enterprise);
    ButterKnife.bind(this);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().addFlags(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    presenter = new EnterpriseLoginPresenter(this);

    buttonLogin.setOnClickListener(v -> {
      String domainUrl = getFromTextInputLayout(loginUrl);
      String token = getFromTextInputLayout(loginToken);
      if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(domainUrl)) {
        login(token, domainUrl);
      }
    });
  }

  private void login(String token, String domainUrl) {
    presenter.login(token, domainUrl);
  }

  @Override
  public void willLogin() {
    buttonLogin.setEnabled(false);
  }

  @Override
  public void onGenericError() {
    Toast.makeText(this, "Error login", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void finishAccess(final User user) {
    setResult(Activity.RESULT_OK);

    MainActivity.startActivity(this);
    finish();
  }

  @Override
  public void didLogin() {
    buttonLogin.setEnabled(true);
    if (buttonLogin != null) {
      KeyboardUtils.lowerKeyboard(this);
    }
  }

  public String getFromTextInputLayout(TextInputLayout inputLayout) {
    if (inputLayout != null && inputLayout.getEditText() != null && inputLayout.getEditText().getText() != null) {
      return inputLayout.getEditText().getText().toString();
    }
    return null;
  }

  @Override
  protected void onStart() {
    super.onStart();
    presenter.start(this);
  }

  @Override
  protected void onStop() {
    presenter.stop();
    super.onStop();
  }
}

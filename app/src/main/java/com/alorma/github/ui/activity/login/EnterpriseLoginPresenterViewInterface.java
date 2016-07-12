package com.alorma.github.ui.activity.login;

import com.alorma.github.sdk.bean.dto.response.User;

public interface EnterpriseLoginPresenterViewInterface {

  void willLogin();

  void onGenericError();

  void finishAccess(User user);

  void didLogin();

  class NullView implements EnterpriseLoginPresenterViewInterface {

    @Override
    public void willLogin() {

    }

    @Override
    public void onGenericError() {

    }

    @Override
    public void finishAccess(User user) {

    }

    @Override
    public void didLogin() {

    }
  }
}
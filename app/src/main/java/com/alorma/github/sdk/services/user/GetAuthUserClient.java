package com.alorma.github.sdk.services.user;

import android.util.Log;
import com.alorma.github.sdk.bean.dto.response.User;
import com.alorma.github.sdk.services.client.GithubClient;
import com.alorma.gitskarios.core.Pair;
import java.util.List;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.Response;
import rx.Observable;

public class GetAuthUserClient extends GithubClient<Pair<User, String>> {
  private String accessToken;

  public GetAuthUserClient() {
    super();
  }

  public GetAuthUserClient(String accessToken) {
    super();
    this.accessToken = accessToken;
  }

  @Override
  protected Observable<Pair<User, String>> getApiObservable(RestAdapter restAdapter) {
    return restAdapter.create(UsersService.class).me().onErrorResumeNext(throwable -> {
      if (throwable instanceof RetrofitError) {
        Response response = ((RetrofitError) throwable).getResponse();
        if (response != null && response.getStatus() == 401) {
          return Observable.error(new UnauthorizedException());
        }
      }
      return Observable.error(throwable);
    }).map(user -> new Pair<>(user, getToken()));
  }

  @Override
  public void log(String message) {
    Log.i("RETROFIIT", message);
  }

  @Override
  protected String getToken() {
    if (accessToken != null) {
      return accessToken;
    } else {
      return super.getToken();
    }
    /*}*/
  }
}

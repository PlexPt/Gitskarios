package com.alorma.github.ui.fragment.events;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.alorma.github.BuildConfig;
import com.alorma.github.IntentsManager;
import com.alorma.github.R;
import com.alorma.github.sdk.bean.dto.response.Commit;
import com.alorma.github.sdk.bean.dto.response.GithubEvent;
import com.alorma.github.sdk.bean.dto.response.Issue;
import com.alorma.github.sdk.bean.dto.response.User;
import com.alorma.github.sdk.bean.dto.response.events.EventType;
import com.alorma.github.sdk.bean.dto.response.events.payload.ForkEventPayload;
import com.alorma.github.sdk.bean.dto.response.events.payload.IssueCommentEventPayload;
import com.alorma.github.sdk.bean.dto.response.events.payload.IssueEventPayload;
import com.alorma.github.sdk.bean.dto.response.events.payload.PullRequestEventPayload;
import com.alorma.github.sdk.bean.dto.response.events.payload.PushEventPayload;
import com.alorma.github.sdk.bean.dto.response.events.payload.ReleaseEventPayload;
import com.alorma.github.sdk.bean.info.RepoInfo;
import com.alorma.github.sdk.services.client.GithubListClient;
import com.alorma.github.sdk.services.user.events.GetUserEventsClient;
import com.alorma.github.ui.activity.RepoDetailActivity;
import com.alorma.github.ui.adapter.events.EventAdapter;
import com.alorma.github.ui.fragment.base.LoadingListFragment;
import com.alorma.github.ui.utils.DialogUtils;
import com.alorma.github.ui.view.UserAvatarView;
import com.alorma.gitskarios.core.Pair;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.google.gson.Gson;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.octicons_typeface_library.Octicons;
import io.fabric.sdk.android.Fabric;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class EventsListFragment extends LoadingListFragment<EventAdapter> implements EventAdapter.EventAdapterListener {

  protected String username;

  private ArrayStrings filterNames;
  private ArrayIntegers filterIds;
  private Observer<List<GithubEvent>> subscriber = new Observer<List<GithubEvent>>() {
    @Override
    public void onCompleted() {
      stopRefresh();
    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onNext(List<GithubEvent> event) {
      if (getAdapter() != null) {
        if (refreshing) {
          getAdapter().clear();
          refreshing = false;
        }
        getAdapter().addAll(event);
      }
    }
  };

  public static EventsListFragment newInstance(String username) {
    Bundle bundle = new Bundle();
    bundle.putString(USERNAME, username);

    EventsListFragment f = new EventsListFragment();
    f.setArguments(bundle);

    return f;
  }

  @Override
  protected int getLightTheme() {
    return R.style.AppTheme_Events;
  }

  @Override
  protected int getDarkTheme() {
    return R.style.AppTheme_Dark_Events;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    getSavedFilter();
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    addNewAdapter();
  }

  protected EventAdapter addNewAdapter() {
    EventAdapter eventAdapter = new EventAdapter(getActivity(), LayoutInflater.from(getActivity()));
    eventAdapter.setEventAdapterListener(this);
    setAdapter(eventAdapter);
    return eventAdapter;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.events_list_fragment, menu);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);

    if (getActivity() != null) {
      MenuItem item = menu.findItem(R.id.events_list_filter);
      if (item != null) {
        item.setIcon(new IconicsDrawable(getActivity(), CommunityMaterial.Icon.cmd_filter_outline).colorRes(R.color.white)
            .sizeDp(24)
            .respectFontBounds(true));
      }
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
      case R.id.events_list_filter:
        EventType[] values = EventType.values();

        String[] names = new String[values.length - 1];

        for (int i = 0; i < values.length; i++) {
          if (values[i] != EventType.Unhandled) {
            names[i] = values[i].name();
          }
        }

        Integer[] ids = null;

        if (filterIds != null) {
          ids = filterIds.toArray(new Integer[filterIds.size()]);
        }

        logAnswers("EVENT_FILTER_CLICK");

        new DialogUtils().builder(getActivity()).items(names).itemsCallbackMultiChoice(ids, (dialog1, which, text) -> {
          EventsListFragment.this.filterIds = new ArrayIntegers(Arrays.asList(which));
          List<CharSequence> filterNames1 = Arrays.asList(text);
          List<String> filters = new ArrayList<>(filterNames1.size());
          for (CharSequence filterName : filterNames1) {
            filters.add(String.valueOf(filterName));
          }
          EventsListFragment.this.filterNames = new ArrayStrings(filters);
          saveFilter();
          executeFromFilter();
          logAnswers("EVENT_FILTER_APPLIED");
          return false;
        }).positiveText(R.string.ok).neutralText(R.string.clear_filters).callback(new MaterialDialog.ButtonCallback() {
          @Override
          public void onNeutral(MaterialDialog dialog) {
            super.onNeutral(dialog);
            EventsListFragment.this.filterIds = null;
            EventsListFragment.this.filterNames = null;
            clearSavedFilter();
            executeFromFilter();
            logAnswers("EVENT_FILTER_CLEAR");
          }
        }).show();
        break;
    }

    return true;
  }

  private void logAnswers(String event) {
    if (BuildConfig.DEBUG && Fabric.isInitialized()) {
      Answers.getInstance().logContentView(new ContentViewEvent().putContentName(event));
    }
  }

  public void getSavedFilter() {
    SharedPreferences shared = getActivity().getSharedPreferences("FILTERS", Context.MODE_PRIVATE);

    Gson gson = new Gson();

    ArrayList events_filter = gson.fromJson(shared.getString("EVENTS_FILTER", null), ArrayList.class);

    if (events_filter != null) {
      EventsListFragment.this.filterNames = new ArrayStrings();
      for (Object o : events_filter) {
        EventsListFragment.this.filterNames.add(String.valueOf(o));
      }
    }

    ArrayList events_filter_ids = gson.fromJson(shared.getString("EVENTS_FILTER_IDS", null), ArrayList.class);

    if (events_filter_ids != null) {
      EventsListFragment.this.filterIds = new ArrayIntegers();
      for (Object o : events_filter_ids) {
        EventsListFragment.this.filterIds.add(Double.valueOf(String.valueOf(o)).intValue());
      }
    }
  }

  private void saveFilter() {
    if (filterNames != null && filterIds != null) {
      SharedPreferences shared = getActivity().getSharedPreferences("FILTERS", Context.MODE_PRIVATE);

      Gson gson = new Gson();
      SharedPreferences.Editor edit = shared.edit();
      edit.putString("EVENTS_FILTER", gson.toJson(new ArrayStrings(filterNames)));
      edit.putString("EVENTS_FILTER_IDS", gson.toJson(new ArrayIntegers(filterIds)));
      edit.apply();
    }
  }

  private void clearSavedFilter() {
    SharedPreferences shared = getActivity().getSharedPreferences("FILTERS", Context.MODE_PRIVATE);

    SharedPreferences.Editor edit = shared.edit();
    edit.remove("EVENTS_FILTER");
    edit.remove("EVENTS_FILTER_IDS");
    edit.apply();
  }

  private void executeFromFilter() {
    startRefresh();
    addNewAdapter();
    executeRequest();
  }

  private boolean filterEvent(GithubEvent event) {
    return !(filterNames != null && !filterNames.isEmpty()) || filterNames.contains(event.type.name());
  }

  @Override
  public void onResume() {
    super.onResume();

    getActivity().setTitle(R.string.menu_events);
  }

  @Override
  protected void loadArguments() {
    username = getArguments().getString(USERNAME);
  }

  @Override
  protected void executeRequest() {
    super.executeRequest();
    GetUserEventsClient eventsClient = new GetUserEventsClient(username);
    executeClient(eventsClient);
  }

  @Override
  protected void executePaginatedRequest(int page) {
    super.executePaginatedRequest(page);

    GetUserEventsClient eventsClient = new GetUserEventsClient(username, page);
    executeClient(eventsClient);
  }

  protected void executeClient(GithubListClient<List<GithubEvent>> eventsClient) {
    eventsClient.observable()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .map(new Func1<Pair<List<GithubEvent>, Integer>, List<GithubEvent>>() {
          @Override
          public List<GithubEvent> call(Pair<List<GithubEvent>, Integer> listIntegerPair) {
            setPage(listIntegerPair.second);
            return listIntegerPair.first;
          }
        })
        .flatMap(Observable::from)
        .filter(this::filterEvent)
        .toList()
        .subscribe(subscriber);
  }

  @Override
  protected Octicons.Icon getNoDataIcon() {
    return Octicons.Icon.oct_calendar;
  }

  @Override
  protected int getNoDataText() {
    return R.string.noevents;
  }

  @Override
  public void onItem(GithubEvent item) {
    EventType type = item.getType();
    Gson gson = new Gson();
    if (type == EventType.IssueCommentEvent) {
      String s = gson.toJson(item.payload);
      IssueCommentEventPayload payload = gson.fromJson(s, IssueCommentEventPayload.class);
      Issue issue = payload.issue;
      if (issue != null) {
        startActivity(new IntentsManager(getActivity()).checkUri(Uri.parse(issue.html_url)));
      }
    } else if (type == EventType.PushEvent) {
      String payload = gson.toJson(item.payload);
      PushEventPayload pushEventPayload = gson.fromJson(payload, PushEventPayload.class);
      if (pushEventPayload != null && pushEventPayload.commits != null) {
        if (pushEventPayload.commits.size() == 1) {
          Commit commit = pushEventPayload.commits.get(0);
          startActivity(new IntentsManager(getActivity()).checkUri(Uri.parse(commit.url)));
        } else if (pushEventPayload.commits.size() > 1) {
          showCommitsDialog(pushEventPayload.commits);
        }
      }
    } else if (type == EventType.IssuesEvent) {
      String payload = gson.toJson(item.payload);
      IssueEventPayload issueEventPayload = gson.fromJson(payload, IssueEventPayload.class);
      if (issueEventPayload != null) {
        startActivity(new IntentsManager(getActivity()).checkUri(Uri.parse(issueEventPayload.issue.html_url)));
      }
    } else if (type == EventType.PullRequestEvent) {
      String payload = gson.toJson(item.payload);
      PullRequestEventPayload pullRequestEventPayload = gson.fromJson(payload, PullRequestEventPayload.class);
      if (pullRequestEventPayload != null) {
        startActivity(new IntentsManager(getActivity()).checkUri(Uri.parse(pullRequestEventPayload.pull_request.html_url)));
      }
    } else if (type == EventType.ForkEvent) {
      String payload = gson.toJson(item.payload);
      ForkEventPayload forkEventPayload = gson.fromJson(payload, ForkEventPayload.class);
      if (forkEventPayload != null) {
        String parentRepo = item.repo.name;
        String forkeeRepo = forkEventPayload.forkee.full_name;

        showReposDialogDialog(parentRepo, forkeeRepo);
      }
    } else if (type == EventType.ReleaseEvent) {
      String payload = gson.toJson(item.payload);
      ReleaseEventPayload releaseEventPayload = gson.fromJson(payload, ReleaseEventPayload.class);
      if (releaseEventPayload != null) {
        Intent intent = new IntentsManager(getActivity()).checkUri(Uri.parse(releaseEventPayload.release.url));

        if (intent != null) {
          startActivity(intent);
        }
      }
    } else {
      // TODO manage TAGs
      if (item.repo.url != null) {
        startActivity(new IntentsManager(getActivity()).manageRepos(Uri.parse(item.repo.url)));
      }
    }
  }

  private void showCommitsDialog(List<Commit> commits) {
    final CommitsAdapter adapter = new CommitsAdapter(getActivity(), commits);
    MaterialDialog.Builder builder = new DialogUtils().builder(getActivity());
    builder.title(R.string.event_select_commit);
    builder.adapter(adapter, (materialDialog, view, i, charSequence) -> {
      Commit item = adapter.getItem(i);

      startActivity(new IntentsManager(getActivity()).checkUri(Uri.parse(item.url)));
    });
    builder.show();
  }

  private void showReposDialogDialog(final String... repos) {

    MaterialDialog.Builder builder = new DialogUtils().builder(getActivity());
    builder.title(R.string.event_select_repository);
    builder.items(repos);
    builder.alwaysCallSingleChoiceCallback();
    builder.itemsCallbackSingleChoice(-1, (materialDialog, view, i, charSequence) -> {
      String repoSelected = repos[i];
      String[] split = repoSelected.split("/");
      RepoInfo repoInfo = new RepoInfo();
      repoInfo.owner = split[0];
      repoInfo.name = split[1];

      Intent intent = RepoDetailActivity.createLauncherIntent(getActivity(), repoInfo);
      startActivity(intent);
      return true;
    });

    builder.show();
  }

  private class ArrayStrings extends ArrayList<String> {

    public ArrayStrings(List<String> filterNames) {
      super(filterNames);
    }

    public ArrayStrings() {

    }
  }

  private class ArrayIntegers extends ArrayList<Integer> {

    public ArrayIntegers(List<Integer> filterIds) {
      super(filterIds);
    }

    public ArrayIntegers() {

    }
  }

  private class CommitsAdapter extends ArrayAdapter<Commit> {

    private final LayoutInflater mInflater;

    public CommitsAdapter(Context context, List<Commit> objects) {
      super(context, 0, objects);
      this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View view = mInflater.inflate(R.layout.row_commit, parent, false);

      ViewHolder holder = new ViewHolder(view);

      Commit commit = getItem(position);

      User author = commit.author;

      if (author == null) {
        author = commit.commit.author;
      }

      if (author == null) {
        author = commit.commit.committer;
      }

      if (author != null) {

        holder.avatar.setUser(author);

        if (author.login != null) {
          holder.user.setText(author.login);
        } else if (author.name != null) {
          holder.user.setText(author.name);
        } else if (author.email != null) {
          holder.user.setText(author.email);
        }
      }

      String message = commit.shortMessage();
      if (commit.commit != null && commit.commit.shortMessage() != null) {
        message = commit.commit.shortMessage();
      }

      holder.title.setText(message);

      if (commit.sha != null) {
        holder.sha.setText(commit.shortSha());
      }

      if (commit.files != null && commit.files.size() > 0) {
        holder.numFiles.setVisibility(View.VISIBLE);
        holder.numFiles.setText(holder.itemView.getContext().getString(R.string.num_of_files, commit.files.size()));
      } else {
        holder.numFiles.setVisibility(View.GONE);
      }

      bindVerification(holder, commit);

      return view;
    }

    private void bindVerification(ViewHolder holder, Commit commit) {
      boolean verifiedCommit = commit.isCommitVerified();
      holder.verifiedCommit.setVisibility(verifiedCommit ? View.VISIBLE : View.GONE);
    }

    public class ViewHolder {

      private final TextView title;
      private final TextView user;
      private final TextView sha;
      private final TextView numFiles;
      private final UserAvatarView avatar;
      private final ImageView verifiedCommit;
      private View itemView;

      public ViewHolder(final View itemView) {
        this.itemView = itemView;
        title = (TextView) itemView.findViewById(R.id.title);
        user = (TextView) itemView.findViewById(R.id.user);
        sha = (TextView) itemView.findViewById(R.id.sha);
        numFiles = (TextView) itemView.findViewById(R.id.numFiles);
        avatar = (UserAvatarView) itemView.findViewById(R.id.avatarAuthor);
        verifiedCommit = (ImageView) itemView.findViewById(R.id.verifiedCommit);
      }
    }
  }
}

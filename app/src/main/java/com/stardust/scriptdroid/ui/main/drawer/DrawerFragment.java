package com.stardust.scriptdroid.ui.main.drawer;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.stardust.notification.NotificationListenerService;
import com.stardust.scriptdroid.App;
import com.stardust.scriptdroid.Pref;
import com.stardust.scriptdroid.R;
import com.stardust.scriptdroid.network.GlideApp;
import com.stardust.scriptdroid.network.UserService;
import com.stardust.scriptdroid.pluginclient.DevPluginClient;
import com.stardust.scriptdroid.ui.floating.CircularMenu;
import com.stardust.scriptdroid.ui.floating.FloatyWindowManger;
import com.stardust.scriptdroid.network.NodeBB;
import com.stardust.scriptdroid.network.VersionService;
import com.stardust.scriptdroid.network.api.UserApi;
import com.stardust.scriptdroid.network.entity.user.User;
import com.stardust.scriptdroid.network.entity.VersionInfo;
import com.stardust.scriptdroid.tool.SimpleObserver;
import com.stardust.scriptdroid.ui.main.community.CommunityFragment;
import com.stardust.scriptdroid.ui.user.LoginActivity_;
import com.stardust.scriptdroid.ui.settings.SettingsActivity;
import com.stardust.scriptdroid.ui.update.UpdateInfoDialogBuilder;
import com.stardust.scriptdroid.ui.user.WebActivity;
import com.stardust.scriptdroid.ui.user.WebActivity_;
import com.stardust.scriptdroid.ui.widget.AvatarView;
import com.stardust.theme.ThemeColorManager;
import com.stardust.theme.ThemeColorManagerCompat;
import com.stardust.view.accessibility.AccessibilityService;
import com.stardust.scriptdroid.pluginclient.DevPluginService;
import com.stardust.scriptdroid.tool.AccessibilityServiceTool;
import com.stardust.scriptdroid.tool.WifiTool;
import com.stardust.util.IntentUtil;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


/**
 * Created by Stardust on 2017/1/30.
 * TODO these codes are so ugly!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 */
@EFragment(R.layout.fragment_drawer)
public class DrawerFragment extends android.support.v4.app.Fragment {

    private static final String URL_SUBLIME_PLUGIN_HELP = "https://github.com/hyb1996/AutoJs-Sublime-Plugin/blob/master/Readme.md";

    @ViewById(R.id.header)
    View mHeaderView;
    @ViewById(R.id.username)
    TextView mUserName;
    @ViewById(R.id.avatar)
    AvatarView mAvatar;
    @ViewById(R.id.shadow)
    View mShadow;
    @ViewById(R.id.default_cover)
    View mDefaultCover;
    @ViewById(R.id.drawer_menu)
    RecyclerView mDrawerMenu;


    private DrawerMenuItem mConnectionItem = new DrawerMenuItem(R.drawable.ic_debug, R.string.debug, 0, this::connectOrDisconnectToRemote);
    private DrawerMenuItem mAccessibilityServiceItem = new DrawerMenuItem(R.drawable.ic_service_green, R.string.text_accessibility_service, 0, this::enableOrDisableAccessibilityService);
    private DrawerMenuItem mNotificationPermissionItem = new DrawerMenuItem(R.drawable.ic_ali_notification, R.string.text_notification_permission, 0, this::goToNotificationServiceSettings);
    private DrawerMenuItem mFloatingWindowItem = new DrawerMenuItem(R.drawable.ic_robot_64, R.string.text_floating_window, 0, this::showOrDismissFloatingWindow);
    private DrawerMenuItem mCheckForUpdatesItem = new DrawerMenuItem(R.drawable.ic_check_for_updates, R.string.text_check_for_updates, this::checkForUpdates);

    private DrawerMenuAdapter mDrawerMenuAdapter;
    private Disposable mConnectionStateDisposable;
    private CommunityDrawerMenu mCommunityDrawerMenu = new CommunityDrawerMenu();


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConnectionStateDisposable = DevPluginService.getInstance().getConnection()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> {
                    if (mConnectionItem != null) {
                        setChecked(mConnectionItem, state.getState() == DevPluginClient.State.CONNECTED);
                        setProgress(mConnectionItem, state.getState() == DevPluginClient.State.CONNECTING);
                    }
                    if (state.getException() != null) {
                        showMessage(state.getException().getMessage());
                    }
                });
        EventBus.getDefault().register(this);

    }

    @AfterViews
    void setUpViews() {
        ThemeColorManager.addViewBackground(mHeaderView);
        initMenuItems();
        if (Pref.isFloatingMenuShown()) {
            setChecked(mFloatingWindowItem, true);
        }
        setChecked(mConnectionItem, DevPluginService.getInstance().isConnected());
    }

    private void initMenuItems() {
        mDrawerMenuAdapter = new DrawerMenuAdapter(new ArrayList<>(Arrays.asList(
                new DrawerMenuGroup(R.string.text_service),
                mAccessibilityServiceItem,
                new DrawerMenuItem(R.drawable.ic_stable, R.string.text_stable_mode, R.string.key_stable_mode, null),
                mNotificationPermissionItem,

                new DrawerMenuGroup(R.string.text_script_record),
                mFloatingWindowItem,
                new DrawerMenuItem(R.drawable.ic_volume, R.string.text_volume_down_control, R.string.key_use_volume_control_record, null),

                new DrawerMenuGroup(R.string.text_others),
                mConnectionItem,
                new DrawerMenuItem(R.drawable.ic_personalize, R.string.text_theme_color, this::openThemeColorSettings),
                mCheckForUpdatesItem
        )));
        mDrawerMenu.setAdapter(mDrawerMenuAdapter);
        mDrawerMenu.setLayoutManager(new LinearLayoutManager(getContext()));
    }


    @Click(R.id.avatar)
    void loginOrShowUserInfo() {
        UserService.getInstance()
                .me()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((user ->
                                WebActivity_.intent(this)
                                        .extra(WebActivity.EXTRA_URL, NodeBB.url("user/" + user.getUserslug()))
                                        .extra(Intent.EXTRA_TITLE, user.getUsername())
                                        .start()),
                        error -> LoginActivity_.intent(getActivity()).start());
    }


    void enableOrDisableAccessibilityService(DrawerMenuItemViewHolder holder) {
        boolean isAccessibilityServiceEnabled = isAccessibilityServiceEnabled();
        boolean checked = holder.getSwitchCompat().isChecked();
        if (checked && !isAccessibilityServiceEnabled) {
            enableAccessibilityService();
        } else if (!checked && isAccessibilityServiceEnabled) {
            if (!AccessibilityService.disable()) {
                AccessibilityServiceTool.goToAccessibilitySetting();
            }
        }
    }

    void goToNotificationServiceSettings(DrawerMenuItemViewHolder holder) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return;
        }
        boolean enabled = NotificationListenerService.getInstance() != null;
        boolean checked = holder.getSwitchCompat().isChecked();
        if ((checked && !enabled) || (!checked && enabled)) {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
    }

    void showOrDismissFloatingWindow(DrawerMenuItemViewHolder holder) {
        boolean isFloatingWindowShowing = FloatyWindowManger.isCircularMenuShowing();
        boolean checked = holder.getSwitchCompat().isChecked();
        if (getActivity() != null && !getActivity().isFinishing()) {
            Pref.setFloatingMenuShown(checked);
        }
        if (checked && !isFloatingWindowShowing) {
            FloatyWindowManger.showCircularMenu();
            enableAccessibilityServiceByRootIfNeeded();
        } else if (!checked && isFloatingWindowShowing) {
            FloatyWindowManger.hideCircularMenu();
        }
    }

    void openThemeColorSettings(DrawerMenuItemViewHolder holder) {
        SettingsActivity.selectThemeColor(getActivity());
    }

    private void enableAccessibilityServiceByRootIfNeeded() {
        Observable.fromCallable(() -> Pref.shouldEnableAccessibilityServiceByRoot() && !isAccessibilityServiceEnabled())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(needed -> {
                    if (needed) {
                        enableAccessibilityServiceByRoot();
                    }
                });

    }

    void connectOrDisconnectToRemote(DrawerMenuItemViewHolder holder) {
        boolean checked = holder.getSwitchCompat().isChecked();
        boolean connected = DevPluginService.getInstance().isConnected();
        if (checked && !connected) {
            inputRemoteHost();
        } else if (!checked && connected) {
            DevPluginService.getInstance().disconnectIfNeeded();
        }
    }

    private void inputRemoteHost() {
        String host = Pref.getServerAddressOrDefault(WifiTool.getRouterIp(getActivity()));
        new MaterialDialog.Builder(getActivity())
                .title(R.string.text_server_address)
                .input("", host, (dialog, input) -> {
                    Pref.saveServerAddress(input.toString());
                    DevPluginService.getInstance().connectToServer(input.toString());
                })
                .neutralText(R.string.text_help)
                .onNeutral((dialog, which) -> {
                    setChecked(mConnectionItem, false);
                    IntentUtil.browse(getActivity(), URL_SUBLIME_PLUGIN_HELP);
                })
                .cancelListener(dialog -> setChecked(mConnectionItem, false))
                .show();
    }

    void checkForUpdates(DrawerMenuItemViewHolder holder) {
        setProgress(mCheckForUpdatesItem, true);
        VersionService.getInstance().checkForUpdates()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<VersionInfo>() {

                    @Override
                    public void onNext(@io.reactivex.annotations.NonNull VersionInfo versionInfo) {
                        if (getActivity() == null)
                            return;
                        if (versionInfo.isNewer()) {
                            new UpdateInfoDialogBuilder(getActivity(), versionInfo)
                                    .show();
                        } else {
                            Toast.makeText(App.getApp(), R.string.text_is_latest_version, Toast.LENGTH_SHORT).show();
                        }
                        setProgress(mCheckForUpdatesItem, false);
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(App.getApp(), R.string.text_check_update_error, Toast.LENGTH_SHORT).show();
                        setProgress(mCheckForUpdatesItem, false);
                    }
                });
    }


    @Override
    public void onResume() {
        super.onResume();
        syncSwitchState();
        syncUserInfo();
    }

    private void syncUserInfo() {
        NodeBB.getInstance().getRetrofit()
                .create(UserApi.class)
                .me()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setUpUserInfo, error -> {
                    error.printStackTrace();
                    setUpUserInfo(null);
                });
    }

    private void setUpUserInfo(@Nullable User user) {
        if (user == null) {
            mUserName.setText(R.string.not_login);
            mAvatar.setIcon(R.drawable.profile_avatar_placeholder);
        } else {
            mUserName.setText(user.getUsername());
            mAvatar.setUser(user);
        }
        setCoverImage(user);


    }

    private void setCoverImage(User user) {
        if (user == null || TextUtils.isEmpty(user.getCoverUrl()) || user.getCoverUrl().equals("/assets/images/cover-default.png")) {
            mDefaultCover.setVisibility(View.VISIBLE);
            mShadow.setVisibility(View.GONE);
            mHeaderView.setBackgroundColor(ThemeColorManagerCompat.getColorPrimary());
        } else {
            mDefaultCover.setVisibility(View.GONE);
            mShadow.setVisibility(View.VISIBLE);
            GlideApp.with(getContext())
                    .load(NodeBB.BASE_URL + user.getCoverUrl())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(new SimpleTarget<Drawable>() {
                        @Override
                        public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                            mHeaderView.setBackground(resource);
                        }
                    });
        }
    }

    private void syncSwitchState() {
        setChecked(mAccessibilityServiceItem, AccessibilityServiceTool.isAccessibilityServiceEnabled(getActivity()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setChecked(mNotificationPermissionItem, NotificationListenerService.getInstance() != null);
        }

    }

    private void enableAccessibilityService() {
        if (!Pref.shouldEnableAccessibilityServiceByRoot()) {
            AccessibilityServiceTool.goToAccessibilitySetting();
            return;
        }
        enableAccessibilityServiceByRoot();
    }

    private void enableAccessibilityServiceByRoot() {
        setProgress(mAccessibilityServiceItem, true);
        Observable.fromCallable(() -> AccessibilityServiceTool.enableAccessibilityServiceByRootAndWaitFor(4000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(succeed -> {
                    if (!succeed) {
                        Toast.makeText(getContext(), R.string.text_enable_accessibitliy_service_by_root_failed, Toast.LENGTH_SHORT).show();
                        AccessibilityServiceTool.goToAccessibilitySetting();
                    }
                    setProgress(mAccessibilityServiceItem, false);
                });
    }


    @Subscribe
    public void onCircularMenuStateChange(CircularMenu.StateChangeEvent event) {
        setChecked(mFloatingWindowItem, event.getCurrentState() != CircularMenu.STATE_CLOSED);
    }

    @Subscribe
    public void onCommunityPageVisibilityChange(CommunityFragment.VisibilityChange change) {
        if (change.visible) {
            mCommunityDrawerMenu.showCommunityMenu(mDrawerMenuAdapter);
        } else {
            mCommunityDrawerMenu.hideCommunityMenu(mDrawerMenuAdapter);
        }
        mDrawerMenu.scrollToPosition(0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoginStateChange(UserService.LoginStateChange change) {
        syncUserInfo();
        if (mCommunityDrawerMenu.isShown()) {
            mCommunityDrawerMenu.setUserOnlineStatus(mDrawerMenuAdapter, change.isOnline());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mConnectionStateDisposable.dispose();
        EventBus.getDefault().unregister(this);
    }


    private void showMessage(CharSequence text) {
        if (getContext() == null)
            return;
        Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
    }


    private void setProgress(DrawerMenuItem item, boolean progress) {
        item.setProgress(progress);
        mDrawerMenuAdapter.notifyItemChanged(item);
    }

    private void setChecked(DrawerMenuItem item, boolean checked) {
        item.setChecked(checked);
        mDrawerMenuAdapter.notifyItemChanged(item);
    }

    private boolean isAccessibilityServiceEnabled() {
        return AccessibilityServiceTool.isAccessibilityServiceEnabled(getActivity());
    }

}

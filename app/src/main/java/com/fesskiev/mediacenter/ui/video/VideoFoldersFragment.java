package com.fesskiev.mediacenter.ui.video;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fesskiev.mediacenter.MediaApplication;
import com.fesskiev.mediacenter.R;
import com.fesskiev.mediacenter.data.model.VideoFolder;
import com.fesskiev.mediacenter.data.source.DataRepository;
import com.fesskiev.mediacenter.services.FileSystemService;
import com.fesskiev.mediacenter.ui.walkthrough.PermissionFragment;
import com.fesskiev.mediacenter.utils.AppAnimationUtils;
import com.fesskiev.mediacenter.utils.AppLog;
import com.fesskiev.mediacenter.utils.AppSettingsManager;
import com.fesskiev.mediacenter.utils.CacheManager;
import com.fesskiev.mediacenter.utils.RxUtils;
import com.fesskiev.mediacenter.utils.Utils;
import com.fesskiev.mediacenter.widgets.dialogs.MediaFolderDetailsDialog;
import com.fesskiev.mediacenter.widgets.dialogs.SimpleDialog;
import com.fesskiev.mediacenter.widgets.dialogs.VideoFolderDetailsDialog;
import com.fesskiev.mediacenter.widgets.item.VideoFolderCardView;
import com.fesskiev.mediacenter.widgets.menu.ContextMenuManager;
import com.fesskiev.mediacenter.widgets.menu.FolderContextMenu;
import com.fesskiev.mediacenter.widgets.recycleview.ItemOffsetDecoration;
import com.fesskiev.mediacenter.widgets.recycleview.helper.ItemTouchHelperAdapter;
import com.fesskiev.mediacenter.widgets.recycleview.helper.ItemTouchHelperViewHolder;
import com.fesskiev.mediacenter.widgets.recycleview.helper.SimpleItemTouchHelperCallback;
import com.fesskiev.mediacenter.widgets.swipe.ScrollChildSwipeRefreshLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;;
import io.reactivex.disposables.Disposable;
import io.reactivex.android.schedulers.AndroidSchedulers;;
import io.reactivex.schedulers.Schedulers;

import static com.fesskiev.mediacenter.ui.walkthrough.PermissionFragment.PERMISSION_REQ;


public class VideoFoldersFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    public static VideoFoldersFragment newInstance() {
        return new VideoFoldersFragment();
    }

    public static final String EXTRA_VIDEO_FOLDER = "com.fesskiev.player.extra.EXTRA_VIDEO_FOLDER";

    private Disposable subscription;
    private DataRepository repository;

    private VideoFoldersAdapter adapter;
    private RecyclerView recyclerView;
    private CardView emptyVideoContent;
    private ScrollChildSwipeRefreshLayout swipeRefreshLayout;

    private boolean layoutAnimate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        repository = MediaApplication.getInstance().getRepository();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video_folders, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 2);

        final int spacing = getResources().getDimensionPixelOffset(R.dimen.default_spacing_small);

        recyclerView = view.findViewById(R.id.foldersGridView);
        recyclerView.setLayoutManager(gridLayoutManager);
        adapter = new VideoFoldersAdapter(getActivity());
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new ItemOffsetDecoration(spacing));

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        emptyVideoContent = view.findViewById(R.id.emptyVideoContentCard);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getContext(), R.color.primary_light));
        swipeRefreshLayout.setProgressViewOffset(false, 0,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));
        swipeRefreshLayout.setScrollUpChild(recyclerView);

    }

    @Override
    public void onResume() {
        super.onResume();
        fetchVideoFolders();
    }

    private void fetchVideoFolders() {
        RxUtils.unsubscribe(subscription);
        subscription = repository.getVideoFolders()
                .firstOrError()
                .toObservable()
                .subscribeOn(Schedulers.io())
                .flatMap(Observable::fromIterable)
                .filter(folder -> AppSettingsManager.getInstance().isShowHiddenFiles() || !folder.isHidden)
                .toList()
                .toObservable()
                .flatMap(videoFolders -> {
                    if (videoFolders != null && !videoFolders.isEmpty()) {
                        Collections.sort(videoFolders);
                    }
                    return videoFolders != null ? Observable.just(videoFolders) : Observable.empty();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(videoFolders -> {
                    if (videoFolders != null) {
                        if (!videoFolders.isEmpty()) {
                            hideEmptyContentCard();
                        } else {
                            showEmptyContentCard();
                        }
                        adapter.refresh(videoFolders);
                        animateLayout();
                    } else {
                        showEmptyContentCard();
                    }
                    AppLog.INFO("onNext:video folders: " + (videoFolders == null ? "null" : videoFolders.size()));
                });
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        RxUtils.unsubscribe(subscription);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        repository.getMemorySource().setCacheVideoFoldersDirty(true);
    }

    @Override
    public void onRefresh() {
        makeRefreshDialog();
    }

    private void makeRefreshDialog() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.addToBackStack(null);
        SimpleDialog dialog = SimpleDialog.newInstance(getString(R.string.dialog_refresh_video_title),
                getString(R.string.dialog_refresh_video_message), R.drawable.icon_refresh);
        dialog.show(transaction, SimpleDialog.class.getName());
        dialog.setPositiveListener(this::checkPermissionAndFetch);
        dialog.setNegativeListener(() -> swipeRefreshLayout.setRefreshing(false));
    }

    private void checkPermissionAndFetch() {
        swipeRefreshLayout.setRefreshing(false);
        if (Utils.isMarshmallow() && !checkPermission()) {
            requestPermission();
        } else {
           fetchFileSystemVideo();
        }
    }

    private void fetchFileSystemVideo() {
        RxUtils.unsubscribe(subscription);
        subscription = RxUtils.fromCallable(repository.resetVideoContentDatabase())
                .subscribeOn(Schedulers.io())
                .doOnNext(integer -> CacheManager.clearVideoImagesCache())
                .subscribe(aVoid -> FileSystemService.startFetchVideo(getActivity()));
    }

    public boolean checkPermission() {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private void requestPermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_REQ);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQ: {
                if (grantResults != null && grantResults.length > 0) {
                    if (PermissionFragment.checkPermissionsResultGranted(grantResults)) {
                        fetchFileSystemVideo();
                    } else  {
                        boolean showRationale =
                                shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        if (showRationale) {
                            permissionsDenied();
                        } else {
                            createExplanationPermissionDialog();
                        }
                    }
                }
                break;
            }
        }
    }

    private void createExplanationPermissionDialog() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.addToBackStack(null);
        SimpleDialog dialog = SimpleDialog.newInstance(getString(R.string.dialog_permission_title),
                getString(R.string.dialog_permission_message), R.drawable.icon_permission_settings);
        dialog.show(transaction, SimpleDialog.class.getName());
        dialog.setPositiveListener(() -> Utils.startSettingsActivity(getContext()));
        dialog.setNegativeListener(() -> getActivity().finish());
    }

    private void permissionsDenied() {
        Utils.showCustomSnackbar(getActivity().getWindow().getDecorView().findViewById(android.R.id.content),
                getContext().getApplicationContext(),
                getString(R.string.snackbar_permission_title), Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_permission_button, v -> requestPermission())
                .show();
    }


    public void refreshVideoContent() {
        swipeRefreshLayout.setRefreshing(false);

        repository.getMemorySource().setCacheVideoFoldersDirty(true);

        fetchVideoFolders();
    }

    public void clearVideoContent() {
        adapter.clearAdapter();
    }

    public SwipeRefreshLayout getSwipeRefreshLayout() {
        return swipeRefreshLayout;
    }

    private void animateLayout() {
        if (!layoutAnimate) {
            AppAnimationUtils.getInstance().loadGridRecyclerItemAnimation(recyclerView);
            recyclerView.scheduleLayoutAnimation();
            layoutAnimate = true;
        }
    }

    private static class VideoFoldersAdapter extends RecyclerView.Adapter<VideoFoldersAdapter.ViewHolder> implements ItemTouchHelperAdapter {

        private WeakReference<Activity> activity;
        private List<VideoFolder> videoFolders;


        public VideoFoldersAdapter(Activity activity) {
            this.activity = new WeakReference<>(activity);
            this.videoFolders = new ArrayList<>();
        }


        public class ViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {

            VideoFolderCardView folderCard;

            public ViewHolder(View v) {
                super(v);

                folderCard = v.findViewById(R.id.videoFolderCardView);
                folderCard.setOnVideoFolderCardViewListener(new VideoFolderCardView.OnVideoFolderCardViewListener() {
                    @Override
                    public void onPopupMenuButtonCall(View view) {
                        showAudioContextMenu(view, getAdapterPosition());
                    }

                    @Override
                    public void onOpenVideoListCall(View view) {
                        startVideoFilesActivity(getAdapterPosition());
                    }
                });
            }

            @Override
            public void onItemSelected() {
                itemView.setAlpha(0.5f);
                changeSwipeRefreshState(false);
            }

            @Override
            public void onItemClear(int position) {
                itemView.setAlpha(1.0f);
                changeSwipeRefreshState(true);
                updateVideoFoldersIndexes();
            }

            private void changeSwipeRefreshState(boolean enable) {
                Activity act = activity.get();
                if (act != null) {
                    VideoFoldersFragment videoFoldersFragment
                            = (VideoFoldersFragment) ((FragmentActivity) act).getSupportFragmentManager().
                            findFragmentByTag(VideoFoldersFragment.class.getName());
                    if (videoFoldersFragment != null) {
                        videoFoldersFragment.getSwipeRefreshLayout().setEnabled(enable);
                    }
                }
            }
        }

        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            Collections.swap(videoFolders, fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onItemDismiss(int position) {

        }

        private void showAudioContextMenu(View view, int position) {
            ContextMenuManager.getInstance().toggleFolderContextMenu(view,
                    new FolderContextMenu.OnFolderContextMenuListener() {
                        @Override
                        public void onDeleteFolder() {
                            deleteVideoFolder(position);
                        }


                        @Override
                        public void onDetailsFolder() {
                            showDetailsVideoFolder(position);
                        }

                        @Override
                        public void onSearchAlbum() {

                        }
                    }, false);
        }

        private void deleteVideoFolder(int position) {
            Activity act = activity.get();
            if (act != null) {
                VideoFolder videoFolder = videoFolders.get(position);
                if (videoFolder != null) {
                    FragmentTransaction transaction =
                            ((FragmentActivity) act).getSupportFragmentManager().beginTransaction();
                    transaction.addToBackStack(null);
                    SimpleDialog dialog = SimpleDialog.newInstance(act.getString(R.string.dialog_delete_file_title),
                            act.getString(R.string.dialog_delete_folder_message), R.drawable.icon_trash);
                    dialog.show(transaction, SimpleDialog.class.getName());
                    dialog.setPositiveListener(() -> Observable.just(CacheManager.deleteDirectoryWithFiles(videoFolder.folderPath))
                            .subscribeOn(Schedulers.io())
                            .flatMap(result -> {
                                if (result) {
                                    DataRepository repository = MediaApplication.getInstance().getRepository();
                                    repository.getMemorySource().setCacheVideoFoldersDirty(true);
                                    return RxUtils.fromCallable(repository.deleteVideoFolder(videoFolder));
                                }
                                return Observable.empty();
                            })
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(integer -> {
                                removeFolder(position);
                                refreshVideoContent(act);
                                Utils.showCustomSnackbar(act.getCurrentFocus(),
                                        act,
                                        act.getString(R.string.shackbar_delete_folder),
                                        Snackbar.LENGTH_LONG)
                                        .show();

                            }));
                }
            }
        }

        private void showDetailsVideoFolder(int position) {
            Activity act = activity.get();
            if (act != null) {
                VideoFolder videoFolder = videoFolders.get(position);
                if (videoFolder != null) {
                    FragmentTransaction transaction =
                            ((FragmentActivity) act).getSupportFragmentManager().beginTransaction();
                    transaction.addToBackStack(null);
                    MediaFolderDetailsDialog dialog = VideoFolderDetailsDialog.newInstance(videoFolder);
                    dialog.setOnMediaFolderDetailsDialogListener(() -> refreshVideoContent(act));
                    dialog.show(transaction, VideoFolderDetailsDialog.class.getName());
                }
            }
        }

        private void refreshVideoContent(Activity act) {
            VideoFoldersFragment videoFoldersFragment = (VideoFoldersFragment) ((FragmentActivity) act)
                    .getSupportFragmentManager().findFragmentByTag(VideoFoldersFragment.class.getName());
            if (videoFoldersFragment != null) {
                videoFoldersFragment.refreshVideoContent();
            }
        }


        private void startVideoFilesActivity(int position) {
            final VideoFolder videoFolder = videoFolders.get(position);
            if (videoFolder != null) {
                Activity act = activity.get();
                if (act != null) {
                    Intent i = new Intent(act, VideoFilesActivity.class);
                    i.putExtra(EXTRA_VIDEO_FOLDER, videoFolder);
                    act.startActivity(i);
                }
            }
        }


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_video_folder, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Activity act = activity.get();
            if (act != null) {

                final VideoFolder videoFolder = videoFolders.get(position);
                if (videoFolder != null) {

                    holder.folderCard.setDescription(videoFolder.folderName);

                    MediaApplication.getInstance().getRepository().getVideoFilesFrame(videoFolder.id)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .take(4)
                            .subscribe(paths -> holder.folderCard.setFrameViewPaths(paths));

                    if (videoFolder.isHidden) {
                        holder.folderCard.setAlpha(0.35f);
                    } else {
                        holder.folderCard.setAlpha(1f);
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            return videoFolders.size();
        }

        public void refresh(List<VideoFolder> receiveVideoFolders) {
            videoFolders.clear();
            videoFolders.addAll(receiveVideoFolders);
            notifyDataSetChanged();
        }

        public void clearAdapter() {
            videoFolders.clear();
            notifyDataSetChanged();
        }

        public void removeFolder(int position) {
            videoFolders.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, getItemCount());
        }

        private void updateVideoFoldersIndexes() {
            RxUtils.fromCallable(MediaApplication.getInstance().getRepository()
                    .updateVideoFoldersIndex(videoFolders))
                    .firstOrError()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(integer -> notifyDataSetChanged());

        }
    }

    private void showEmptyContentCard() {
        emptyVideoContent.setVisibility(View.VISIBLE);
    }

    private void hideEmptyContentCard() {
        emptyVideoContent.setVisibility(View.GONE);
    }

}

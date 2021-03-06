package com.bigfat.guessmusic.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bigfat.guessmusic.R;
import com.bigfat.guessmusic.adapter.WordGridAdapter;
import com.bigfat.guessmusic.constant.Constant;
import com.bigfat.guessmusic.listener.IAlertDialogButtonListener;
import com.bigfat.guessmusic.listener.WordClickListener;
import com.bigfat.guessmusic.model.Song;
import com.bigfat.guessmusic.model.Word;
import com.bigfat.guessmusic.service.AudioService;
import com.bigfat.guessmusic.util.DataUtil;
import com.bigfat.guessmusic.util.LogUtil;
import com.bigfat.guessmusic.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity implements View.OnClickListener, WordClickListener {

    public static final String TAG = "MainActivity";

    public static final int ID_DIALOG_DELETE_WORD = 1;
    public static final int ID_DIALOG_TIP_ANSWER = 2;
    public static final int ID_DIALOG_LACK_COINS = 3;

    //控件
    private ImageButton mBtnPlayStart;//播放按钮
    private ImageView mViewPan;//唱片盘片
    private ImageView mViewPanBar;//唱片拨杆
    private GridView mWordGridView;//待选文字
    private LinearLayout mSelectedWordsContainer;//已选文字按钮的容器
    private ArrayList<Button> mSelectedButtonList;//已选文字按钮
    private TextView mTvCurrentStage;//当前关索引
    private TextView mTvCurrentCoins;//当前金币数量
    private ImageButton mBtnTopBack;//返回按钮

    //浮动按钮
    private ImageButton mBtnDeleteWord;//删除待选文字
    private ImageButton mBtnTipAnswer;//提示

    //过关界面
    private LinearLayout mPassView;
    private TextView mCurrentStagePassView;//过关界面关卡索引
    private TextView mCurrentSongNamePassView;//过关界面歌曲名称
    private TextView mRewardConis;//过关奖励金币数
    private ImageButton mBtnNext;//下一关
    private ImageButton mBtnShare;//分享到微信

    //数据
    private WordGridAdapter mWordAdapter;
    private ArrayList<Word> mAllWords;//待选文字
    private ArrayList<Word> mSelectedWords;//已选文字

    //唱片相关动画
    private Animation mPanAnim;
    private LinearInterpolator mPanLin;

    private Animation mBarInAnim;
    private LinearInterpolator mBarInLin;

    private Animation mBarOutAnim;
    private LinearInterpolator mBarOutLin;

    //状态量
    private boolean mIsRunning = false;//唱片旋转动画是否正在播放
    private Song mCurrentSong;//当前播放歌曲
    private int mCurrentStageIndex = -1;//当前关卡索引
    private int mCurrentCoins = Constant.TOTAL_COINS;//当前金币数量
    /**
     * 删除一个待选字
     */
    private IAlertDialogButtonListener mBtnOkDeleteWordListener = new IAlertDialogButtonListener() {
        @Override
        public void onOkButtonClick() {
            deleteOneWord();
        }
    };
    /**
     * 提示一个答案
     */
    private IAlertDialogButtonListener mBtnOkTipAnswerListener = new IAlertDialogButtonListener() {
        @Override
        public void onOkButtonClick() {
            tipOneAnswerWord();
        }
    };
    /**
     * 金币不足
     */
    private IAlertDialogButtonListener mBtnOkLackCoinsListener = new IAlertDialogButtonListener() {
        @Override
        public void onOkButtonClick() {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //读取游戏数据
        int[] data = DataUtil.loadData(MainActivity.this);
        mCurrentStageIndex = data[Constant.INDEX_SAVE_DATA_STAGE];
        mCurrentCoins = data[Constant.INDEX_SAVE_DATA_COIN];

        initAnim();//初始化动画
        initAnimListener();

        initView();
        initViewListener();

        initCurrentStageData();
    }

    @Override
    protected void onPause() {
        mViewPan.clearAnimation();
        Utils.stopSong(MainActivity.this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        //存储游戏数据
        DataUtil.saveData(MainActivity.this, mCurrentStageIndex - 1, mCurrentCoins);
        stopService(new Intent(MainActivity.this, AudioService.class));
        super.onStop();
    }

    /**
     * 初始化当前关卡数据
     */
    private void initCurrentStageData() {
        //读取当前关歌曲信息
        mCurrentSong = loadStageSongInfo(++mCurrentStageIndex);
        //初始化已选文字
        if (mSelectedWords == null) {
            mSelectedWords = new ArrayList<>();
        } else {
            mSelectedWords.clear();
        }
        mSelectedWords.addAll(getSelectedWords());
        //初始化待选文字
        if (mAllWords == null) {
            mAllWords = new ArrayList<>();
        } else {
            mAllWords.clear();
        }
        mAllWords.addAll(getAllWords());

        //显示数据
        mTvCurrentStage.setText(String.valueOf(mCurrentStageIndex + 1));//显示关卡索引
        mTvCurrentCoins.setText(String.valueOf(mCurrentCoins));//设置当前金币
        initSelectedWordsView();//初始化已选文字View
        //初始化待选文字
        if (mWordAdapter == null) {
            mWordAdapter = new WordGridAdapter(MainActivity.this, mAllWords);
            mWordAdapter.setWordClickListener(this);
            mWordGridView.setAdapter(mWordAdapter);
        } else {
            mWordAdapter.notifyDataSetChanged();
            mWordGridView.scheduleLayoutAnimation();
        }

        //播放当前歌曲
        handlePlayButtonEvent();
    }

    private void initAnim() {
        mPanAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        mPanLin = new LinearInterpolator();
        mPanAnim.setInterpolator(mPanLin);

        mBarInAnim = AnimationUtils.loadAnimation(this, R.anim.rotate_45);
        mBarInLin = new LinearInterpolator();
        mBarInAnim.setInterpolator(mBarInLin);

        mBarOutAnim = AnimationUtils.loadAnimation(this, R.anim.rotate_d_45);
        mBarOutLin = new LinearInterpolator();
        mBarOutAnim.setInterpolator(mBarOutLin);
    }

    private void initAnimListener() {
        mPanAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mViewPanBar.startAnimation(mBarOutAnim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mBarInAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mViewPan.startAnimation(mPanAnim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mBarOutAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mIsRunning = false;
                mBtnPlayStart.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void initView() {
        mBtnPlayStart = (ImageButton) findViewById(R.id.btn_play_start);
        mViewPan = (ImageView) findViewById(R.id.imageView1);
        mViewPanBar = (ImageView) findViewById(R.id.imageView2);
        mWordGridView = (GridView) findViewById(R.id.name_select_grid_view);
        mSelectedWordsContainer = (LinearLayout) findViewById(R.id.word_selected_container);
        mPassView = (LinearLayout) findViewById(R.id.pass_view);
        mTvCurrentStage = (TextView) findViewById(R.id.text_current_stage);
        mRewardConis = (TextView) findViewById(R.id.text_reward_conis);
        mTvCurrentCoins = (TextView) findViewById(R.id.tv_top_bar_coin);
        mBtnTopBack = (ImageButton) findViewById(R.id.btn_top_bar_back);
        mBtnDeleteWord = (ImageButton) findViewById(R.id.btn_delete_word);
        mBtnTipAnswer = (ImageButton) findViewById(R.id.btn_tip_answer);
        mCurrentStagePassView = (TextView) findViewById(R.id.text_current_stage_pass);
        mCurrentSongNamePassView = (TextView) findViewById(R.id.text_current_song_name_pass);
        mBtnNext = (ImageButton) findViewById(R.id.btn_next);
        mBtnShare = (ImageButton) findViewById(R.id.btn_share);

        mRewardConis.setText(String.valueOf(Constant.REWARD_COINS));
    }

    private void initSelectedWordsView() {
        if (mSelectedButtonList == null) {
            mSelectedButtonList = new ArrayList<>();
        } else {
            mSelectedButtonList.clear();
        }
        mSelectedWordsContainer.removeAllViews();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(140, 140);
        for (int i = 0; i < mSelectedWords.size(); i++) {
            final int j = i;
            Button wordButton = (Button) LayoutInflater.from(MainActivity.this).inflate(R.layout.word_grid_view_item, null);
            wordButton.setTextColor(Color.WHITE);
            wordButton.setText("");
            wordButton.setBackgroundResource(R.mipmap.game_wordblank);
            wordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSelectedWordClick(j);
                }
            });

            mSelectedButtonList.add(wordButton);
            mSelectedWordsContainer.addView(wordButton, params);
        }
    }

    private void initViewListener() {
        mBtnPlayStart.setOnClickListener(this);
        mBtnTopBack.setOnClickListener(this);
        mBtnDeleteWord.setOnClickListener(this);
        mBtnTipAnswer.setOnClickListener(this);
        mBtnNext.setOnClickListener(this);
        mBtnShare.setOnClickListener(this);
    }

    //自定义AlertDialog响应事件

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play_start://播放
                handlePlayButtonEvent();
                break;

            case R.id.btn_top_bar_back://返回
                finish();
                break;

            case R.id.btn_delete_word://删除一个待选字
                showConfirmDialog(ID_DIALOG_DELETE_WORD);
                break;

            case R.id.btn_tip_answer://提示一个答案
                showConfirmDialog(ID_DIALOG_TIP_ANSWER);
                break;

            case R.id.btn_next://下一关
                if (isHaveNextStage()) {//还有下一关
                    mPassView.setVisibility(View.GONE);//隐藏通关界面
                    //加载下一关数据
                    initCurrentStageData();
                } else {//没有下一关，通关
                    //重置关卡及金币数
                    mCurrentStageIndex = 0;
                    mCurrentCoins = Constant.TOTAL_COINS;
                    //跳转到通关界面
                    Intent intent = new Intent(MainActivity.this, AllPassActivity.class);
                    startActivity(intent);
                    finish();
                }
                break;

            case R.id.btn_share://分享到微信

                break;
        }
    }

    /**
     * 播放按钮点击
     */
    private void handlePlayButtonEvent() {
        if (mViewPanBar != null) {
            if (!mIsRunning) {
                mViewPanBar.startAnimation(mBarInAnim);
                mIsRunning = true;
                mBtnPlayStart.setVisibility(View.INVISIBLE);
            }
        }
        Utils.playSong(MainActivity.this, mCurrentSong.getFileName());
    }

    @Override
    public void onWordClick(Button wordButton, Word word) {
        setSelectedWord(word);
    }

    /**
     * 获得关卡歌曲信息
     */
    private Song loadStageSongInfo(int stageIndex) {
        Song song = new Song();
        String[] stage = Constant.SONG_INFO[stageIndex];
        song.setFileName(stage[Constant.INDEX_SONG_INFO_FILE_NAME]);
        song.setName(stage[Constant.INDEX_SONG_INFO_NAME]);
        return song;
    }

    /**
     * 获得所有待选文字
     */
    private ArrayList<Word> getAllWords() {
        ArrayList<Word> data = new ArrayList<>();
        //加入当前歌曲文字
        for (int i = 0; i < mSelectedWords.size(); i++) {
            Word word = new Word();
            word.setText(mSelectedWords.get(i).getText());
            data.add(word);
        }
        //加入随机汉字
        for (int i = mSelectedWords.size(); i < Constant.WORD_COUNT; i++) {
            Word word = new Word();
            word.setText(Utils.getRandomHanzi());
            data.add(word);
        }
        //打乱顺序
        Collections.shuffle(data, new Random());
        return data;
    }

    /**
     * 获得所有已选文字
     */
    private ArrayList<Word> getSelectedWords() {
        ArrayList<Word> data = new ArrayList<>();
        for (char songNameChar : mCurrentSong.getNameCharacters()) {
            Word word = new Word();
            word.setText(String.valueOf(songNameChar));
            word.setVisiable(false);
            data.add(word);
        }
        return data;
    }

    /**
     * 设置已选文字
     */
    private void setSelectedWord(Word word) {
        for (int i = 0; i < mSelectedWords.size(); i++) {
            if (TextUtils.isEmpty(mSelectedButtonList.get(i).getText())) {//如果已选文字框有空位，则将点选的文字显示在已选文字框中
                mSelectedWords.get(i).setIndex(word.getIndex());
                mSelectedButtonList.get(i).setText(word.getText());
                word.setVisiable(false);
                mWordAdapter.notifyDataSetChanged();
                checkAnswer();//检验答案
                break;
            }
        }
    }

    /**
     * 已选文字框被点击
     */
    private void onSelectedWordClick(int position) {
        if (!TextUtils.isEmpty(mSelectedButtonList.get(position).getText())) {
            mAllWords.get(mSelectedWords.get(position).getIndex()).setVisiable(true);
            mWordAdapter.notifyDataSetChanged();
            mSelectedButtonList.get(position).setText("");
        }
    }

    /**
     * 检查答案
     */
    private void checkAnswer() {
        if (isAnswerComplete()) {//答案完整，继续检查答案是否正确
            if (isAnswerCorrect()) {//答案正确
                handlePassEvent();//过关
            } else {//答案错误
                splashTheWords();//文字闪烁
            }
        } else {//答案不完整，设置字体为白色
            for (Button wordButton : mSelectedButtonList) {
                wordButton.setTextColor(Color.WHITE);
            }
        }
    }

    /**
     * 答案是否完整
     */
    private boolean isAnswerComplete() {
        //检验已选字是否填满
        for (Button wordButton : mSelectedButtonList) {
            if (TextUtils.isEmpty(wordButton.getText())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 答案是否正确
     */
    private boolean isAnswerCorrect() {
        StringBuilder sb = new StringBuilder();
        for (Button wordButton : mSelectedButtonList) {
            sb.append(wordButton.getText());
        }
        return sb.toString().equals(mCurrentSong.getName());
    }

    /**
     * 已选文字闪烁
     */
    private void splashTheWords() {
        final Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            private int splashedTimes = 0;//闪烁次数
            private boolean isRed = true;//是否显示红色

            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (++splashedTimes > Constant.SPLASH_TIMES) {//闪烁6次以上则停止
                            timer.cancel();
                        }
                        for (Button wordButton : mSelectedButtonList) {
                            wordButton.setTextColor(isRed ? Color.RED : Color.WHITE);
                        }
                        isRed = !isRed;
                    }
                });
            }
        };
        timer.schedule(task, 0, 150);
    }

    /**
     * 处理过关界面及事件
     */
    private void handlePassEvent() {
        Utils.stopSong(MainActivity.this);//停止播放歌曲
        mViewPan.clearAnimation();//停止动画
        mPassView.setVisibility(View.VISIBLE);//显示过关界面
        mCurrentStagePassView.setText(String.valueOf(mCurrentStageIndex + 1));
        mCurrentSongNamePassView.setText(mCurrentSong.getName());
        handleCoins(Constant.REWARD_COINS);//过关金币奖励
        Utils.playTone(MainActivity.this, Constant.INDEX_TONE_COIN);//播放金币掉落音效
    }

    /**
     * 删除一个文字
     */
    private void deleteOneWord() {
        //扣除金币
        if (!handleCoins(-Utils.getDeleteWordCoins())) {//扣除失败
            //金币不足，显示对话框
            showConfirmDialog(ID_DIALOG_LACK_COINS);
            return;
        }

        //删除一个待选文字
        mAllWords.get(findCanDeleteWordIndex()).setVisiable(false);
        mWordAdapter.notifyDataSetChanged();
    }

    /**
     * 找到一个可以删除的文字的索引
     */
    private int findCanDeleteWordIndex() {
        Random rand = new Random();
        while (true) {
            int index = rand.nextInt(Constant.WORD_COUNT);
            LogUtil.i(TAG, "index--->" + index);
            //可以删除的字需要满足两点： 1.未隐藏 2.不是答案文字的字
            if (mAllWords.get(index).isVisiable() &&
                    !mCurrentSong.getName().contains(mAllWords.get(index).getText())) {
                return index;
            }
        }
    }

    /**
     * 提示一个答案文字
     */
    private void tipOneAnswerWord() {
        //找到一个空白的已选文字框
        int selectedBlankIndex = findBlankInSelectedWord();
        if (selectedBlankIndex == -1) {//没有空白框
            splashTheWords();
            Toast.makeText(MainActivity.this, "没有空位", Toast.LENGTH_SHORT).show();
            return;
        }

        //扣除金币
        if (!handleCoins(-Utils.getTipAnswerCoins())) {//扣除失败
            //金币不足，显示对话框
            showConfirmDialog(ID_DIALOG_LACK_COINS);
            return;
        }

        //填充空白位置对应的答案文字
        int index = findAnswerWord(String.valueOf(mCurrentSong.getNameCharacters()[selectedBlankIndex]));
        if (index != -1) {
            setSelectedWord(mAllWords.get(index));
        }
    }

    /**
     * 从已选文字框中找到一个空的
     *
     * @return 返回空白位置的索引; -1 没有空白位置
     */
    private int findBlankInSelectedWord() {
        for (int i = 0; i < mSelectedButtonList.size(); i++) {
            if (TextUtils.isEmpty(mSelectedButtonList.get(i).getText())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 找到答案文字在待选文字中的索引
     *
     * @return -1 未找到
     */
    private int findAnswerWord(String text) {
        for (Word word : mAllWords) {
            //可提示答案文字需要满足：1.未隐藏2.是对应答案文字
            if (word.isVisiable() && word.getText().equals(text)) {
                return word.getIndex();
            }
        }
        return -1;
    }

    /**
     * 增加/减少指定数量的金币
     *
     * @return true 增加/减少成功
     * false 失败
     */
    private boolean handleCoins(int count) {
        if (mCurrentCoins + count >= 0) {
            mCurrentCoins += count;
            mTvCurrentCoins.setText(String.valueOf(mCurrentCoins));
            return true;
        } else {//金币不足
            return false;
        }
    }

    /**
     * 是否还有下一关/是否通关
     */
    private boolean isHaveNextStage() {
        return mCurrentStageIndex != Constant.SONG_INFO.length - 1;
    }

    /**
     * 弹出自定义Dialog
     *
     * @param id Dialog类型ID
     */
    private void showConfirmDialog(int id) {
        switch (id) {
            case ID_DIALOG_DELETE_WORD:
                Utils.showCustomeDialog(MainActivity.this,
                        "确定花掉" + Utils.getDeleteWordCoins() + "个金币去掉一个错误答案？", mBtnOkDeleteWordListener);
                break;

            case ID_DIALOG_TIP_ANSWER:
                Utils.showCustomeDialog(MainActivity.this, "确定花掉" + Utils.getTipAnswerCoins() + "个金币获得一个文字提示？", mBtnOkTipAnswerListener);
                break;

            case ID_DIALOG_LACK_COINS:
                Utils.showCustomeDialog(MainActivity.this, "金币不足，补充一下？", mBtnOkLackCoinsListener);
                break;
        }
    }
}
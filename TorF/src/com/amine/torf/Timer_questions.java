package com.amine.torf;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amine.torf.helpers.DataBaseHelper;
import com.amine.torf.helpers.DataManager;
import com.amine.torf.pojo.QuizPojo;
import com.andtinder.model.CardModel;
import com.andtinder.view.CardContainer;
import com.andtinder.view.SimpleCardStackAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.games.Games;
import com.google.android.gms.internal.mz;
import com.google.android.gms.plus.Plus;
import com.google.example.games.basegameutils.BaseGameUtils;

public class Timer_questions extends Activity implements ConnectionCallbacks,
		OnConnectionFailedListener {
	String que, category, comment = "";
	int isTrue, difficulty;
	int rightans = 0;
	int wrongans = 0;
	int i = 0;
	String right, wrong, next;
	DataBaseHelper db;
	TextView txtcategoryname, tv, tanoofque, lifeline;
	static int currentQuestion = 0;
	MediaPlayer mp;
	List<QuizPojo> getquestions = null;
	QuizPojo cn = null;
	MyCounter timer = null;
	Setting_preference pref;
	String no_of_questions, strtimer;
	long savedtimer;
	int totalQueLen;
	boolean cbvibrate, cbtimer;
	Vibrator vibe;
	String categoryname;
	int mistake = DataManager.mistake;
	int numofquestions = DataManager.noofquestions;
	private AdView adView;
	Button btnpass, btntimer;
	private static final String AD_UNIT_ID = DataManager.admobid;
	TextView[] btn1, btn2;
	// Random rand = new Random();
	boolean fifty = false, pass = false, time = false;
	Typeface normal, bold;
	LinearLayout lltimer;
	// //swipe card
	private CardContainer mCardContainerQ, mCardContainerC,
			mCardContainerLIntro, mCardContainerRIntro;
	final static int INTERVAL = 800; // 1 second
	boolean whichColor = true;
	CardModel cardModelQuestion, cardModelComment;
	SimpleCardStackAdapter adapterQuestion, adapterComment;
	LinearLayout linearBoundQ, linearBoundC, linearButtons;

	AccomplishmentsOutbox mOutbox = new AccomplishmentsOutbox();
	private GoogleApiClient mGoogleApiClient;

	public long timerCount = 15000;
	private String TAG = "TRF QUESTIONS";

	@SuppressLint("DefaultLocale")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.quizscreen);
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this).addApi(Plus.API)
				.addScope(Plus.SCOPE_PLUS_LOGIN).addApi(Games.API)
				.addScope(Games.SCOPE_GAMES).build();

		// mGoogleApiClient.connect();
		adapterQuestion = new SimpleCardStackAdapter(this);
		adapterComment = new SimpleCardStackAdapter(this);

		linearBoundQ = (LinearLayout) findViewById(R.id.layoutBoundQ);
		linearBoundC = (LinearLayout) findViewById(R.id.layoutBoundC);
		linearButtons = (LinearLayout) findViewById(R.id.buttons);
		linearBoundC.setVisibility(View.GONE);

		cardModelQuestion = new CardModel("");
		cardModelComment = new CardModel("");

		mCardContainerQ = (CardContainer) findViewById(R.id.layoutCardQuestion);
		mCardContainerC = (CardContainer) findViewById(R.id.layoutCardComment);

		normal = Typeface.createFromAsset(getAssets(), "normal.ttf");
		bold = Typeface.createFromAsset(getAssets(), "bold.ttf");

		tv = (TextView) findViewById(R.id.tv);
		tv.setText(" ");
		db = new DataBaseHelper(getApplicationContext());

		pref = new Setting_preference(getApplicationContext());

		strtimer = DataManager.timer;
		vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		savedtimer = Long.parseLong(strtimer);
		categoryname = getIntent().getStringExtra("categoryname");

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(Timer_questions.this);

		cbvibrate = prefs.getBoolean("cbvibrate", true);
		cbtimer = prefs.getBoolean("cbtimer", true);
		btntimer = (Button) this.findViewById(R.id.btntimer);
		lltimer = (LinearLayout) this.findViewById(R.id.lltimer);

		if (cbtimer) {
			timer = new MyCounter(savedtimer * 1000, 1000);
			timer.start();
		} else {
			tv.setVisibility(View.INVISIBLE);
			btntimer.setVisibility(View.GONE);
			lltimer.setVisibility(View.GONE);
		}

		currentQuestion = 0;
		rightans = 0;
		wrongans = 0;

		lifeline = (TextView) this.findViewById(R.id.txtlife);
		btnpass = (Button) this.findViewById(R.id.btnskip);
		txtcategoryname = (TextView) this.findViewById(R.id.txtcategoryname);
		tanoofque = (TextView) this.findViewById(R.id.tanoofque1);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		right = "Right answer";
		wrong = "Wrong answer";
		next = "Next Question";

		tanoofque.setTypeface(normal);
		lifeline.setTypeface(bold);
		btntimer.setTypeface(bold);
		btnpass.setTypeface(bold);
		txtcategoryname.setTypeface(normal);
		tv.setTypeface(bold);
		totalQueLen = 20;

		getquestions = db.getquestion(categoryname);

		Log.w("questions ", "" + getquestions.get(0).get_isTrue());
		getquestionsanswers(currentQuestion);

		txtcategoryname.setText(categoryname.toUpperCase());
		btntimer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				if (cbtimer) {
					time = true;
					long newtime = 15000;
					timer.cancel();
					timer = new MyCounter(newtime, 1000);
					timer.start();
					btntimer.setVisibility(View.INVISIBLE);
				}
			}
		});

		btnpass.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				pass = true;
				nextquestion(0);
				btnpass.setVisibility(View.INVISIBLE);
			}
		});

		cardModelComment.setTitle("Swipe right if the answer is wright");

		cardModelComment
				.setOnCardDimissedListener(new CardModel.OnCardDimissedListener() {
					public void onRight() {
						Log.i("Swipeable Cards ", "right");

					}

					public void onLeft() {
						Log.i("Swipeable Cards ", "Left");

					}
				});
		adapterComment.add(cardModelComment);
		mCardContainerC.setAdapter(adapterComment);

		lifeline.setText("Life : " + mistake);

		adView = new AdView(this);

		adView.setAdSize(AdSize.BANNER);
		adView.setAdUnitId(AD_UNIT_ID);
		AdRequest adRequest = new AdRequest.Builder().build();

		adView.loadAd(adRequest);
		LinearLayout ll = (LinearLayout) findViewById(R.id.ad);
		ll.addView(adView);

	}

	// ************************Achievements*****************************************//

	void achievementToast(String achievement) {
		// Only show toast if not signed in. If signed in, the standard Google
		// Play
		// toasts will appear, so we don't need to show our own.

		Toast.makeText(this,
				getString(R.string.achievement) + ": " + achievement,
				Toast.LENGTH_LONG).show();

	}

	void checkForAchievements(int finalScore) {
		// Check if each condition is met; if so, unlock the corresponding

		// achievement.
		if (finalScore == 5) {
			mOutbox.achievement_newbie = true;
			achievementToast(getString(R.string.achievement_newbie));
			pushAccomplishments();

		}
		if (finalScore == 10) {
			mOutbox.achievement_rookie = true;
			achievementToast(getString(R.string.achievement_rookie));
			pushAccomplishments();

		}
		if (finalScore == 15) {
			mOutbox.achievement_beginner = true;
			achievementToast(getString(R.string.achievement_beginner));
			pushAccomplishments();

		}
		if (finalScore == 20) {
			mOutbox.achievement_talented = true;
			achievementToast(getString(R.string.achievement_talented));
			pushAccomplishments();

		}
		if (finalScore == 25) {
			mOutbox.achievement_intermediate = true;
			achievementToast(getString(R.string.achievement_intermediate));
			pushAccomplishments();

		}

		if (finalScore <= 20) {
			mOutbox.mEasyModeScore = finalScore;
		} else {
			mOutbox.mHardModeScore = finalScore;
		}

	}

	public void userSwipe(int isTrueAnswer, boolean swipedRight, String comment) {

		if ((isTrueAnswer == 1 && swipedRight)
				|| (isTrueAnswer == 0 && !swipedRight)) {
			rightans++;
			if (rightans % 5 == 0)
				checkForAchievements(rightans);
			linearBoundQ.setBackgroundColor(Color.GREEN);
			nextquestion(500);

		}

		else {
			if (isTrueAnswer == 0 && swipedRight) {

				showComment();
			}

			if (isTrueAnswer == 1 && !swipedRight) {
				nextquestion(500);
				linearBoundQ.setBackgroundColor(Color.RED);
			}

			wrongans++;
			vibrate();
			mistake--;
			lifeline.setText("Life : " + mistake);

		}
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(INTERVAL);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					updateColor();

				}
			}
		}).start();
	}

	public void nextquestion(int SPLASHTIME) {

		if (cbtimer) {
			timer.cancel();
		}
		Handler handler = new Handler();

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {

				db.close();
				currentQuestion++;

				if (mistake > 0) {

					if (currentQuestion < totalQueLen) {
						if (cbtimer) {
							timer = new MyCounter(savedtimer * 1000, 1000);
							timer.start();
						}
						getquestionsanswers(currentQuestion);

					} else {

						if (cbtimer) {
							timer.cancel();
						}

						// update leaderboards

						// push those accomplishments to the cloud, if signed in

						/*
						 * Intent iScore = new Intent(Timer_questions.this,
						 * Score.class); iScore.putExtra("rightans", rightans);
						 * iScore.putExtra("totalques", totalQueLen);
						 * iScore.putExtra("category", category); finish();
						 * startActivity(iScore);
						 */

					}
				} else {
					if (cbtimer) {
						timer.cancel();
					}

					// update leaderboards

					// push those accomplishments to the cloud, if signed in

					/*
					 * Intent iScore = new Intent(Timer_questions.this,
					 * Score.class); iScore.putExtra("rightans", rightans);
					 * iScore.putExtra("totalques", totalQueLen);
					 * iScore.putExtra("category", category); finish();
					 * startActivity(iScore);
					 */

				}

			}

		}, SPLASHTIME);
		updateLeaderboards(rightans);

	}

	public void vibrate() {
		if (cbvibrate) {
			vibe.vibrate(700);
		}
	}

	public void getquestionsanswers(int index) {
		cn = getquestions.get(currentQuestion);
		que = cn.get_question();
		isTrue = cn.get_isTrue();
		comment = cn.get_comment();
		setCurrentQuestion(que, isTrue, comment);
	}

	public void showComment() {

		if (cbtimer)
			timer.cancel();

		linearBoundQ.setVisibility(View.GONE);
		linearButtons.setVisibility(View.INVISIBLE);
		linearBoundC.setVisibility(View.VISIBLE);
		linearBoundC.setBackgroundColor(Color.RED);

		tanoofque.setTextColor(Color.RED);
		tanoofque.setText("Swipe to pass!!");

		Animation anim = new AlphaAnimation(0.0f, 1.0f);
		anim.setDuration(100); // You can manage the time of the blink with this
		// parameter
		anim.setStartOffset(20);
		anim.setRepeatMode(Animation.REVERSE);
		anim.setRepeatCount(Animation.INFINITE);
		tanoofque.startAnimation(anim);

		cardModelComment.setTitle("'" + comment + "'");

		cardModelComment
				.setOnCardDimissedListener(new CardModel.OnCardDimissedListener() {
					public void onRight() {
						Log.i("Swipeable Cards ", "right");
						passComment(0);

					}

					public void onLeft() {
						Log.i("Swipeable Cards ", "Left");
						passComment(0);

					}
				});
		adapterComment.add(cardModelComment);
		mCardContainerC.setAdapter(adapterComment);

		// passComment(2200);

	}

	public void passComment(int timer) {
		linearBoundQ.setVisibility(View.VISIBLE);
		linearBoundC.setVisibility(View.GONE);
		linearButtons.setVisibility(View.VISIBLE);

		nextquestion(timer);

	}

	public void setCurrentQuestion(String question, final int isTrueQuestion,
			final String comment) {

		i++;
		String noofque = "Question No. " + i + " out of " + totalQueLen + " "
				+ isTrueQuestion;
		tanoofque.setTextColor(Color.BLACK);
		tanoofque.setText(noofque);
		tanoofque.clearAnimation();
		cardModelQuestion.setTitle(question);

		// Resources r = getResources();
		Log.w(question, " " + isTrueQuestion);

		cardModelQuestion
				.setOnCardDimissedListener(new CardModel.OnCardDimissedListener() {
					public void onRight() {
						Log.i("Swipeable Cards ", "right");

						if (cbtimer) {
							timer.cancel();
						}

						userSwipe(isTrueQuestion, true, comment);
						db.insertIsanswered(cn.get_id());
					}

					public void onLeft() {
						Log.i("Swipeable Cards ", "Left");

						if (cbtimer) {
							timer.cancel();
						}

						userSwipe(isTrueQuestion, false, comment);
						db.insertIsanswered(cn.get_id());

					}
				});

		adapterQuestion.add(cardModelQuestion);
		mCardContainerQ.setAdapter(adapterQuestion);

		// taQue.setText(que);

	}

	private void updateColor() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				linearBoundQ.setBackgroundResource(R.drawable.background);

			}
		});
	}

	public class MyCounter extends CountDownTimer {

		public MyCounter(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}

		@Override
		public void onFinish() {
			Log.w("Counter", " " + timerCount);
			if (timerCount <= 2000) {
				mistake--;
				lifeline.setText("Life : " + mistake);
				nextquestion(400);
			}

		}

		@SuppressLint("UseValueOf")
		@Override
		public void onTick(long millisUntilFinished) {

			timerCount = millisUntilFinished;
			Integer milisec = new Integer(
					new Double(millisUntilFinished).intValue());

			Integer cd_secs = milisec / 1000;

			Integer seconds = (cd_secs % 3600) % 60;

			tv.setTextColor(Color.BLACK);
			tv.setText("Timer  : " + String.format("%02d", seconds));

			if (seconds < 6) {
				tv.setTextColor(Color.RED);
			}
		}

	}

	@Override
	public void onBackPressed() {
		if (cbtimer) {
			timer.cancel();
		}
		new AlertDialog.Builder(this)
				.setTitle("Really Exit?")
				.setMessage("Do you want to leave this Test?")
				.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface arg0, int arg1) {

								if (cbtimer) {
									timer.cancel();
								}

								timer = new MyCounter(timerCount, 1000);
								timer.start();

							}
						})
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface arg0, int arg1) {

								if (cbtimer) {
									timer.cancel();
								}
								int totalQueLen = db.getContactsCount();
								Intent i = new Intent(Timer_questions.this,
										Score.class);
								i.putExtra("rightans", rightans);
								i.putExtra("totalques", totalQueLen);
								i.putExtra("category", category);
								finish();
								startActivity(i);
							}
						}).create().show();
	}

	@Override
	public void onStart() {
		super.onStart();
		mGoogleApiClient.connect();
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (cbtimer) {
			timer.cancel();

		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		// timer = new MyCounter(timerCount, 1000);
		// timer.start();

	}

	@Override
	protected void onStop() {
		super.onStop();

		if (cbtimer) {
			timer.cancel();
		}
		Log.d(TAG, "onStop(): disconnecting");
		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}

	void pushAccomplishments() {

		if (mOutbox.achievement_newbie) {

			Games.Achievements.unlock(mGoogleApiClient,
					getString(R.string.achievement_newbie));

		}
		if (mOutbox.achievement_rookie) {

			Games.Achievements.unlock(mGoogleApiClient,
					getString(R.string.achievement_rookie));

		}

		if (mOutbox.achievement_beginner) {

			Games.Achievements.unlock(mGoogleApiClient,
					getString(R.string.achievement_beginner));

		}

		if (mOutbox.achievement_talented) {

			Games.Achievements.unlock(mGoogleApiClient,
					getString(R.string.achievement_talented));

		}

		if (mOutbox.achievement_intermediate) {

			Games.Achievements.unlock(mGoogleApiClient,
					getString(R.string.achievement_intermediate));

		}

		/*
		 * if (mOutbox.mPrimeAchievement) {
		 * Games.Achievements.unlock(mGoogleApiClient,
		 * getString(R.string.amazing)); mOutbox.mPrimeAchievement = false; } if
		 * (mOutbox.mHumbleAchievement) {
		 * Games.Achievements.unlock(mGoogleApiClient,
		 * getString(R.string.achievement_humble)); mOutbox.mHumbleAchievement =
		 * false; } if (mOutbox.mLeetAchievement) {
		 * Games.Achievements.unlock(mGoogleApiClient,
		 * getString(R.string.achievement_leet)); mOutbox.mLeetAchievement =
		 * false; } if (mOutbox.mBoredSteps > 0) {
		 * Games.Achievements.increment(mGoogleApiClient,
		 * getString(R.string.achievement_really_bored), mOutbox.mBoredSteps);
		 * Games.Achievements.increment(mGoogleApiClient,
		 * getString(R.string.achievement_bored), mOutbox.mBoredSteps); } if
		 * (mOutbox.mEasyModeScore >= 0) {
		 * Games.Leaderboards.submitScore(mGoogleApiClient,
		 * getString(R.string.leaderboard_easy), mOutbox.mEasyModeScore);
		 * mOutbox.mEasyModeScore = -1; } if (mOutbox.mHardModeScore >= 0) {
		 * Games.Leaderboards.submitScore(mGoogleApiClient,
		 * getString(R.string.leaderboard_hard), mOutbox.mHardModeScore);
		 * mOutbox.mHardModeScore = -1; }
		 */
		if (mOutbox.mEasyModeScore >= 0) {
			Games.Leaderboards.submitScore(mGoogleApiClient,
					getString(R.string.leaderboard_leader_board_beginners),
					mOutbox.mEasyModeScore);
			mOutbox.mEasyModeScore = -1;
		}
		if (mOutbox.mHardModeScore >= 0) {
			Games.Leaderboards.submitScore(mGoogleApiClient,
					getString(R.string.leaderboard_leader_board_advanced),
					mOutbox.mHardModeScore);
			mOutbox.mHardModeScore = -1;
		}
		mOutbox.saveLocal(this);
	}

	/**
	 * Update leaderboards with the user's score.
	 *
	 * @param finalScore
	 *            The score the user got.
	 */
	void updateLeaderboards(int finalScore) {
		mOutbox.score = finalScore;
	}

	class AccomplishmentsOutbox {

		boolean achievement_newbie = false;
		boolean achievement_rookie = false;
		boolean achievement_beginner = false;
		boolean achievement_talented = false;
		boolean achievement_intermediate = false;
		int score = 0;
		int mEasyModeScore = -1;
		int mHardModeScore = -1;

		boolean isEmpty() {
			return !achievement_newbie && !achievement_rookie
					&& !achievement_beginner && !achievement_talented
					&& !achievement_intermediate && score == 0
					&& mEasyModeScore < 0 && mHardModeScore < 0;
		}

		public void saveLocal(Context ctx) {
			/*
			 * TODO: This is left as an exercise. To make it more difficult to
			 * cheat, this data should be stored in an encrypted file! And
			 * remember not to expose your encryption key (obfuscate it by
			 * building it from bits and pieces and/or XORing with another
			 * string, for instance).
			 */
		}

		public void loadLocal(Context ctx) {
			/*
			 * TODO: This is left as an exercise. Write code here that loads
			 * data from the file you wrote in saveLocal().
			 */
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.d(TAG, "onConnectionFailed(): attempting to resolve");

		// TODO Auto-generated method stub
		// Sign-in failed, so show sign-in button on main menu
	}

	@Override
	public void onConnected(Bundle arg0) {
		Log.d(TAG, "onConnected(): connected to Google APIs");
		// Show sign-out button on main menu

		/*
		 * Player p = Games.Players.getCurrentPlayer(mGoogleApiClient); String
		 * displayName; if (p == null) { Log.w(TAG,
		 * "mGamesClient.getCurrentPlayer() is NULL!"); displayName = "???"; }
		 * else { displayName = p.getDisplayName(); }
		 */
		// TODO

		// if we have accomplishments to push, push them
		if (!mOutbox.isEmpty()) {
			pushAccomplishments();
			Toast.makeText(this,
					getString(R.string.your_progress_will_be_uploaded),
					Toast.LENGTH_LONG).show();
		}

	}

	@Override
	public void onConnectionSuspended(int arg0) {
		Log.d(TAG, "onConnectionSuspended(): attempting to connect");
		mGoogleApiClient.connect();
	}

}

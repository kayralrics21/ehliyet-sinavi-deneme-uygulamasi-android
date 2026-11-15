package com.ehliyet.deneme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int NIGHT = 0xFF09090C;
    private static final int PANEL = 0xFF16171C;
    private static final int PANEL_BORDER = 0xFF2D2D34;
    private static final int RED = 0xFFE32D47;
    private static final int RED_DIM = 0xFF4B1C29;
    private static final int WHITE = Color.WHITE;
    private static final int PAPER = 0xFFF9FAFC;
    private static final int INK = 0xFF181B24;
    private static final int MUTED = 0xFF747A88;
    private static final int SOFT = 0xFFE7E9EE;
    private static final int GREEN = 0xFF168451;
    private static final int GREEN_LIGHT = 0xFFE8F8F0;
    private static final int BAD = 0xFFCC3346;
    private static final int BAD_LIGHT = 0xFFFFEDF0;
    private static final long DURATION = 45L * 60L * 1000L;
    private static final String CREDIT = "KAYRA HAN AYDOĞAN";
    private static final String[] LABELS = {"A", "B", "C", "D"};
    private static final Locale TR = new Locale("tr", "TR");

    private enum Screen { HOME, EXAM, RESULT, REVIEW }
    private Screen screen = Screen.HOME;
    private final Handler ticker = new Handler(Looper.getMainLooper());
    private Runnable tick;
    private SharedPreferences prefs;
    private Exam exam;
    private int[] answers;
    private int position;
    private long deadline;
    private TextView timeLabel;
    private TextView progressLabel;
    private ScrollView mainScroll;
    private int resultCorrect;
    private int resultWrong;
    private int resultBlank;

    private static final class Question {
        String text;
        String category;
        String image;
        String[] choices = new String[4];
        int correct;
    }

    private static final class Exam {
        int id;
        ArrayList<Question> questions = new ArrayList<>();
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("ehliyet_offline_v1", MODE_PRIVATE);
        getWindow().setNavigationBarColor(NIGHT);
        home();
    }

    private int dp(float px) {
        return Math.round(getResources().getDisplayMetrics().density * px);
    }

    private GradientDrawable rounded(int fill, int border, int radius) {
        GradientDrawable b = new GradientDrawable();
        b.setColor(fill);
        b.setCornerRadius(dp(radius));
        if (border != 0) b.setStroke(dp(1), border);
        return b;
    }

    private LinearLayout col() {
        LinearLayout x = new LinearLayout(this);
        x.setOrientation(LinearLayout.VERTICAL);
        return x;
    }

    private LinearLayout row() {
        LinearLayout x = new LinearLayout(this);
        x.setOrientation(LinearLayout.HORIZONTAL);
        x.setGravity(Gravity.CENTER_VERTICAL);
        return x;
    }

    private LinearLayout.LayoutParams dims(int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h));
    }

    private void gap(LinearLayout parent, int height) {
        View spacer = new View(this);
        parent.addView(spacer, dims(1, height));
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(true);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        t.setLineSpacing(dp(2), 1.06f);
        return t;
    }

    private TextView pill(String s, int bg, int fg) {
        TextView t = text(s, 11, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(11), dp(7), dp(11), dp(7));
        t.setBackground(rounded(bg, 0, 20));
        return t;
    }

    private TextView button(String label, int bg, int fg, Runnable action) {
        TextView v = text(label, 14, fg, true);
        v.setGravity(Gravity.CENTER);
        v.setMinHeight(dp(51));
        v.setPadding(dp(15), dp(13), dp(15), dp(13));
        v.setBackground(rounded(bg, 0, 13));
        v.setClickable(true);
        v.setFocusable(true);
        v.setOnClickListener(view -> action.run());
        return v;
    }

    private LinearLayout newPage(boolean dark) {
        stopClock();
        getWindow().setStatusBarColor(dark ? NIGHT : WHITE);
        boolean lightNavigation = Build.VERSION.SDK_INT >= 26;
        getWindow().setNavigationBarColor(dark || !lightNavigation ? NIGHT : WHITE);
        int uiFlags = dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (!dark && lightNavigation) uiFlags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(uiFlags);
        LinearLayout root = col();
        root.setBackgroundColor(dark ? NIGHT : WHITE);
        root.setFitsSystemWindows(true);
        root.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        setContentView(root);
        return root;
    }

    private LinearLayout scrollContent(LinearLayout root, boolean dark, int top, int bottom) {
        mainScroll = new ScrollView(this);
        mainScroll.setFillViewport(false);
        mainScroll.setVerticalScrollBarEnabled(false);
        mainScroll.setClipToPadding(false);
        mainScroll.setBackgroundColor(dark ? NIGHT : WHITE);
        root.addView(mainScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = col();
        body.setPadding(dp(20), dp(top), dp(20), dp(bottom));
        mainScroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        return body;
    }

    private Exam loadExam(int id) {
        if (id < 1 || id > 26) return null;
        try (InputStream in = getAssets().open("tests/" + id + ".json"); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            JSONObject data = new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
            Exam e = new Exam();
            e.id = id;
            JSONArray list = data.getJSONArray("questions");
            for (int i = 0; i < list.length(); i++) {
                JSONObject obj = list.getJSONObject(i);
                Question q = new Question();
                q.text = obj.getString("text");
                q.category = obj.getString("category");
                q.image = obj.optString("image", "");
                q.correct = obj.getInt("correct");
                JSONArray options = obj.getJSONArray("choices");
                for (int j = 0; j < 4; j++) q.choices[j] = options.getString(j);
                e.questions.add(q);
            }
            if (e.questions.size() != 50) throw new IllegalStateException("Soru sayısı hatalı");
            return e;
        } catch (Exception error) {
            Toast.makeText(this, "Test dosyası okunamadı.", Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private void home() {
        screen = Screen.HOME;
        exam = null;
        LinearLayout root = newPage(true);
        LinearLayout content = scrollContent(root, true, 33, 24);

        TextView logo = text("EHLİYET", 13, RED, true);
        logo.setLetterSpacing(.24f);
        content.addView(logo);
        gap(content, 12);
        TextView title = text("Sınava hazır\nmısın?", 35, WHITE, true);
        title.setLineSpacing(dp(3), 1.0f);
        content.addView(title);
        gap(content, 9);
        content.addView(text("26 test. 1.300 soru. Tamamen çevrimdışı.", 14, 0xFFACADB5, false));
        gap(content, 23);

        LinearLayout stat = row();
        stat.setPadding(dp(16), dp(17), dp(16), dp(17));
        stat.setBackground(rounded(PANEL, PANEL_BORDER, 17));
        LinearLayout left = col();
        left.addView(text("26", 24, WHITE, true));
        left.addView(text("TEST", 10, 0xFF9697A2, true));
        stat.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout middle = col();
        middle.addView(text("50", 24, WHITE, true));
        middle.addView(text("SORU / TEST", 10, 0xFF9697A2, true));
        stat.addView(middle, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout right = col();
        right.addView(text("35", 24, RED, true));
        right.addView(text("GEÇME SINIRI", 10, 0xFF9697A2, true));
        stat.addView(right, new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(stat);
        gap(content, 29);
        TextView heading = text("DENEME SINAVLARI", 12, 0xFFBBBBBF, true);
        heading.setLetterSpacing(.15f);
        content.addView(heading);
        gap(content, 13);

        for (int id = 1; id <= 26; id++) {
            final int testId = id;
            LinearLayout card = row();
            card.setPadding(dp(15), dp(17), dp(14), dp(17));
            card.setBackground(rounded(PANEL, PANEL_BORDER, 15));
            TextView number = text(String.format(TR, "%02d", id), 19, RED, true);
            number.setGravity(Gravity.CENTER);
            number.setBackground(rounded(RED_DIM, 0, 11));
            card.addView(number, dims(52, 54));
            LinearLayout copy = col();
            copy.setPadding(dp(14), 0, 0, 0);
            copy.addView(text("Test " + id, 16, WHITE, true));
            gap(copy, 3);
            int best = prefs.getInt("best_" + id, -1);
            String info = best < 0 ? "50 soru  •  45 dakika" : "En iyi: " + best + "/50  •  " + (best >= 35 ? "Geçildi" : "Tekrar dene");
            copy.addView(text(info, 12, 0xFFA0A1AB, false));
            card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
            card.addView(text("›", 29, 0xFFB6B6BC, false));
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> selectTest(testId));
            content.addView(card);
            gap(content, 10);
        }
        gap(content, 24);
        TextView credit = text(CREDIT, 11, 0xFF777982, true);
        credit.setLetterSpacing(.12f);
        credit.setGravity(Gravity.CENTER);
        content.addView(credit);
        gap(content, 15);
    }

    private void selectTest(int testId) {
        String saved = prefs.getString("session_" + testId, null);
        if (saved != null) {
            new AlertDialog.Builder(this)
                .setTitle("Test " + testId)
                .setMessage("Bu testte yarım kalan bir sınavın var.")
                .setPositiveButton("Devam et", (d, which) -> resume(testId))
                .setNeutralButton("Yeniden başla", (d, which) -> start(testId))
                .setNegativeButton("İptal", null)
                .show();
        } else {
            String result = prefs.getString("result_" + testId, null);
            if (result != null) {
                new AlertDialog.Builder(this)
                    .setTitle("Test " + testId)
                    .setMessage("Bu testin önceki sonucunu inceleyebilir veya yeniden çözebilirsin.")
                    .setPositiveButton("Sınavı başlat", (d, w) -> start(testId))
                    .setNeutralButton("Son sonucu gör", (d, w) -> previousResult(testId))
                    .setNegativeButton("İptal", null)
                    .show();
            } else start(testId);
        }
    }

    private void start(int testId) {
        Exam e = loadExam(testId);
        if (e == null) return;
        exam = e;
        answers = new int[50];
        for (int i = 0; i < 50; i++) answers[i] = -1;
        position = 0;
        deadline = System.currentTimeMillis() + DURATION;
        screen = Screen.EXAM;
        saveSession();
        displayQuestion(false);
    }

    private String sessionKey() { return "session_" + exam.id; }

    private void saveSession() {
        if (exam == null || answers == null || screen == Screen.RESULT || screen == Screen.REVIEW) return;
        try {
            JSONObject obj = new JSONObject();
            JSONArray a = new JSONArray();
            for (int v : answers) a.put(v);
            obj.put("answers", a);
            obj.put("position", position);
            obj.put("deadline", deadline);
            prefs.edit().putString(sessionKey(), obj.toString()).apply();
        } catch (Exception ignored) { }
    }

    private void resume(int testId) {
        Exam e = loadExam(testId);
        if (e == null) return;
        exam = e;
        answers = new int[50];
        try {
            JSONObject obj = new JSONObject(prefs.getString("session_" + testId, "{}"));
            JSONArray a = obj.getJSONArray("answers");
            if (a.length() != 50) throw new IllegalArgumentException("invalid saved answers");
            for (int i = 0; i < 50; i++) {
                answers[i] = a.getInt(i);
                if (answers[i] < -1 || answers[i] > 3) throw new IllegalArgumentException("invalid answer");
            }
            position = Math.max(0, Math.min(49, obj.optInt("position", 0)));
            deadline = obj.getLong("deadline");
        } catch (Exception error) {
            Toast.makeText(this, "Önceki oturum okunamadı; yeni test açılıyor.", Toast.LENGTH_SHORT).show();
            start(testId);
            return;
        }
        if (deadline <= System.currentTimeMillis()) finish(true);
        else displayQuestion(false);
    }

    private int completed() {
        int n = 0;
        for (int v : answers) if (v >= 0) n++;
        return n;
    }

    private void stopClock() {
        if (tick != null) ticker.removeCallbacks(tick);
        tick = null;
        timeLabel = null;
        progressLabel = null;
    }

    private void startClock() {
        if (screen != Screen.EXAM) return;
        tick = new Runnable() {
            @Override public void run() {
                if (screen != Screen.EXAM || exam == null) return;
                long left = deadline - System.currentTimeMillis();
                if (left <= 0L) { finish(true); return; }
                if (timeLabel != null) {
                    long seconds = (left + 999) / 1000;
                    timeLabel.setText(String.format(TR, "%02d:%02d", seconds / 60, seconds % 60));
                    timeLabel.setTextColor(left < 5L * 60L * 1000L ? BAD : RED);
                }
                ticker.postDelayed(this, 950L);
            }
        };
        ticker.post(tick);
    }

    private void displayQuestion(boolean reviewing) {
        if (exam == null || answers == null) { home(); return; }
        screen = reviewing ? Screen.REVIEW : Screen.EXAM;
        LinearLayout root = newPage(false);
        LinearLayout header = row();
        header.setPadding(dp(17), dp(13), dp(17), dp(13));
        header.setBackgroundColor(WHITE);
        TextView back = text("‹", 32, INK, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> leaveQuestion());
        header.addView(back, dims(42, 45));
        LinearLayout hcopy = col();
        hcopy.setPadding(dp(9), 0, 0, 0);
        hcopy.addView(text("TEST " + exam.id, 17, INK, true));
        hcopy.addView(text(reviewing ? "Cevapları incele" : "Deneme sınavı", 11, MUTED, false));
        header.addView(hcopy, new LinearLayout.LayoutParams(0, -2, 1));
        timeLabel = pill(reviewing ? "SONUÇ" : "45:00", reviewing ? 0xFFF0F1F5 : BAD_LIGHT, reviewing ? INK : RED);
        header.addView(timeLabel);
        root.addView(header);
        View rule = new View(this);
        rule.setBackgroundColor(SOFT);
        root.addView(rule, dims(-1, 1));

        LinearLayout body = scrollContent(root, false, 20, 30);
        LinearLayout top = row();
        progressLabel = text("SORU " + (position + 1) + " / 50", 12, INK, true);
        progressLabel.setLetterSpacing(.1f);
        top.addView(progressLabel, new LinearLayout.LayoutParams(0, -2, 1));
        TextView pick = pill(reviewing ? "Sorulara git" : completed() + "/50 yanıtlandı", PAPER, MUTED);
        pick.setOnClickListener(v -> showPicker(reviewing));
        top.addView(pick);
        body.addView(top);
        gap(body, 14);

        LinearLayout track = row();
        track.setBackground(rounded(SOFT, 0, 5));
        for (int i = 0; i < 50; i++) {
            View segment = new View(this);
            segment.setBackground(rounded(answers[i] >= 0 ? RED : SOFT, 0, 2));
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, dp(4), 1);
            track.addView(segment, pp);
        }
        body.addView(track);
        gap(body, 25);

        Question q = exam.questions.get(position);
        body.addView(pill(q.category, 0xFFF2F3F7, 0xFF585F6E));
        gap(body, 16);
        TextView qt = text(q.text, 20, INK, true);
        qt.setLineSpacing(dp(5), 1.09f);
        body.addView(qt);
        gap(body, 18);

        if (!q.image.isEmpty()) {
            try (InputStream in = getAssets().open("figures/" + q.image)) {
                Bitmap pic = BitmapFactory.decodeStream(in);
                if (pic != null) {
                    ImageView img = new ImageView(this);
                    img.setImageBitmap(pic);
                    img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    img.setAdjustViewBounds(true);
                    img.setMaxHeight(dp(230));
                    img.setBackground(rounded(PAPER, SOFT, 13));
                    LinearLayout.LayoutParams pp = dims(-1, -2);
                    pp.bottomMargin = dp(13);
                    body.addView(img, pp);
                }
            } catch (Exception ignored) { }
        }

        int selected = answers[position];
        for (int j = 0; j < 4; j++) {
            final int answerIndex = j;
            boolean correctOption = (selected >= 0 || reviewing) && q.correct == j;
            boolean wrongOption = selected == j && q.correct != j;
            int fill = correctOption ? GREEN_LIGHT : wrongOption ? BAD_LIGHT : WHITE;
            int stroke = correctOption ? GREEN : wrongOption ? BAD : SOFT;
            int ink = correctOption ? GREEN : wrongOption ? BAD : INK;
            LinearLayout item = row();
            item.setPadding(dp(14), dp(13), dp(13), dp(13));
            item.setBackground(rounded(fill, stroke, 13));
            TextView letter = text(LABELS[j], 15, correctOption || wrongOption ? WHITE : MUTED, true);
            letter.setGravity(Gravity.CENTER);
            letter.setBackground(rounded(correctOption ? GREEN : wrongOption ? BAD : 0xFFF0F1F5, 0, 9));
            item.addView(letter, dims(38, 42));
            TextView copy = text(q.choices[j], 15, ink, correctOption || wrongOption);
            copy.setPadding(dp(12), 0, 0, 0);
            item.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
            if (correctOption) item.addView(text("✓", 20, GREEN, true));
            else if (wrongOption) item.addView(text("✕", 20, BAD, true));
            if (selected < 0 && !reviewing) {
                item.setFocusable(true);
                item.setClickable(true);
                item.setOnClickListener(v -> choose(answerIndex));
            }
            body.addView(item);
            gap(body, 10);
        }

        if (selected >= 0) {
            gap(body, 13);
            boolean correct = selected == q.correct;
            LinearLayout feedback = col();
            feedback.setPadding(dp(15), dp(15), dp(15), dp(15));
            feedback.setBackground(rounded(correct ? GREEN_LIGHT : BAD_LIGHT, 0, 12));
            feedback.addView(text(correct ? "Doğru cevap!" : "Yanlış cevap", 15, correct ? GREEN : BAD, true));
            gap(feedback, 5);
            feedback.addView(text("Doğru şık: " + LABELS[q.correct] + ") " + q.choices[q.correct], 14, INK, false));
            body.addView(feedback);
        } else if (reviewing) {
            gap(body, 10);
            LinearLayout feedback = col();
            feedback.setPadding(dp(15), dp(15), dp(15), dp(15));
            feedback.setBackground(rounded(0xFFF2F3F6, 0, 12));
            feedback.addView(text("Boş bırakılan soru", 15, INK, true));
            gap(feedback, 5);
            feedback.addView(text("Doğru şık: " + LABELS[q.correct] + ") " + q.choices[q.correct], 14, INK, false));
            body.addView(feedback);
        }

        LinearLayout footer = col();
        footer.setPadding(dp(20), dp(12), dp(20), dp(12));
        footer.setBackgroundColor(WHITE);
        View line = new View(this);
        line.setBackgroundColor(SOFT);
        footer.addView(line, dims(-1, 1));
        gap(footer, 12);
        LinearLayout buttons = row();
        TextView prev = button("‹ Önceki", 0xFFF0F1F4, INK, () -> move(-1, reviewing));
        prev.setEnabled(position > 0);
        prev.setAlpha(position > 0 ? 1f : .35f);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, -2, 1);
        p1.rightMargin = dp(10);
        buttons.addView(prev, p1);
        TextView next = button(position == 49 ? (reviewing ? "Sonuca dön" : "Sınavı bitir") : "Sonraki ›", RED, WHITE, () -> {
            if (position == 49) {
                if (reviewing) showResult(); else askFinish();
            } else move(1, reviewing);
        });
        buttons.addView(next, new LinearLayout.LayoutParams(0, -2, 1));
        footer.addView(buttons);
        if (!reviewing) {
            gap(footer, 8);
            TextView finishText = text("Sınavı bitir", 12, MUTED, true);
            finishText.setGravity(Gravity.CENTER);
            finishText.setPadding(dp(8), dp(8), dp(8), dp(8));
            finishText.setOnClickListener(v -> askFinish());
            footer.addView(finishText);
        }
        root.addView(footer);
        if (!reviewing) startClock();
    }

    private void choose(int answer) {
        if (screen != Screen.EXAM || !ExamLogic.canSelect(answers[position]) || System.currentTimeMillis() >= deadline) return;
        answers[position] = answer;
        saveSession();
        displayQuestion(false);
    }

    private void move(int diff, boolean reviewing) {
        position = Math.max(0, Math.min(49, position + diff));
        if (!reviewing) saveSession();
        displayQuestion(reviewing);
    }

    private void showPicker(boolean reviewing) {
        if (exam == null) return;
        String[] opts = new String[50];
        for (int i = 0; i < 50; i++) {
            String state;
            if (answers[i] == -1) state = "○";
            else if (answers[i] == exam.questions.get(i).correct) state = "✓";
            else state = "✕";
            opts[i] = String.format(TR, "%02d", i + 1) + "     " + state + "     " + exam.questions.get(i).category;
        }
        new AlertDialog.Builder(this)
            .setTitle("Sorular")
            .setItems(opts, (dialog, which) -> {
                position = which;
                if (!reviewing) saveSession();
                displayQuestion(reviewing);
            })
            .setNegativeButton("Kapat", null)
            .show();
    }

    private void askFinish() {
        if (screen != Screen.EXAM) return;
        int blank = 50 - completed();
        String message = blank > 0 ? blank + " soruyu boş bıraktın. Sınavı bitirmek istiyor musun?" : "Sınavı tamamlamak istiyor musun?";
        new AlertDialog.Builder(this)
            .setTitle("Sınavı bitir")
            .setMessage(message)
            .setPositiveButton("Bitir", (d, w) -> finish(false))
            .setNegativeButton("Devam et", null)
            .show();
    }

    private void finish(boolean expired) {
        if (exam == null || answers == null) { home(); return; }
        int[] key = new int[50];
        for (int i = 0; i < 50; i++) key[i] = exam.questions.get(i).correct;
        int[] counts = ExamLogic.grade(answers, key);
        int good = counts[0], bad = counts[1], blank = counts[2];
        resultCorrect = good;
        resultWrong = bad;
        resultBlank = blank;
        try {
            JSONObject result = new JSONObject();
            JSONArray a = new JSONArray();
            for (int v : answers) a.put(v);
            result.put("answers", a);
            result.put("correct", good);
            result.put("wrong", bad);
            result.put("blank", blank);
            result.put("expired", expired);
            result.put("date", System.currentTimeMillis());
            int best = Math.max(prefs.getInt("best_" + exam.id, -1), good);
            int attempts = prefs.getInt("attempts_" + exam.id, 0) + 1;
            prefs.edit()
                .putString("result_" + exam.id, result.toString())
                .putInt("best_" + exam.id, best)
                .putInt("attempts_" + exam.id, attempts)
                .remove("session_" + exam.id)
                .apply();
        } catch (Exception ignored) { }
        showResult();
    }

    private void previousResult(int id) {
        Exam e = loadExam(id);
        if (e == null) return;
        try {
            JSONObject res = new JSONObject(prefs.getString("result_" + id, "{}"));
            JSONArray a = res.getJSONArray("answers");
            if (a.length() != 50) throw new IllegalArgumentException();
            exam = e;
            answers = new int[50];
            for (int i = 0; i < 50; i++) answers[i] = a.getInt(i);
            resultCorrect = res.getInt("correct");
            resultWrong = res.getInt("wrong");
            resultBlank = res.getInt("blank");
            position = 0;
            showResult();
        } catch (Exception exception) {
            Toast.makeText(this, "Önceki sonuç okunamadı.", Toast.LENGTH_SHORT).show();
        }
    }

    private void statCard(LinearLayout container, String number, String label, int color, int widthWeight) {
        LinearLayout card = col();
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(7), dp(18), dp(7), dp(18));
        card.setBackground(rounded(PAPER, SOFT, 13));
        TextView value = text(number, 27, color, true);
        value.setGravity(Gravity.CENTER);
        card.addView(value);
        gap(card, 3);
        TextView cap = text(label, 11, MUTED, true);
        cap.setGravity(Gravity.CENTER);
        card.addView(cap);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(0, -2, widthWeight);
        param.rightMargin = dp(8);
        container.addView(card, param);
    }

    private void showResult() {
        if (exam == null) { home(); return; }
        screen = Screen.RESULT;
        LinearLayout root = newPage(false);
        LinearLayout content = scrollContent(root, false, 23, 28);
        TextView small = text("TEST " + exam.id + " / SONUÇ", 12, MUTED, true);
        small.setLetterSpacing(.13f);
        content.addView(small);
        gap(content, 21);
        boolean passed = ExamLogic.passed(resultCorrect);
        TextView flag = pill(passed ? "SINAV GEÇİLDİ" : "SINAV GEÇİLEMEDİ", passed ? GREEN_LIGHT : BAD_LIGHT, passed ? GREEN : BAD);
        content.addView(flag);
        gap(content, 13);
        content.addView(text(resultCorrect * 2 + " / 100", 49, INK, true));
        gap(content, 9);
        String message = passed
            ? "tebrikler, deneme sınavını geçtiniz. - " + CREDIT
            : "maalesef, deneme sınavını kazanamadınız. bir dahaki sefere! - " + CREDIT;
        TextView outcome = text(message, 20, passed ? GREEN : BAD, true);
        outcome.setLineSpacing(dp(4), 1.13f);
        content.addView(outcome);
        gap(content, 24);

        LinearLayout stats = row();
        statCard(stats, String.valueOf(resultCorrect), "DOĞRU", GREEN, 1);
        statCard(stats, String.valueOf(resultWrong), "YANLIŞ", BAD, 1);
        statCard(stats, String.valueOf(resultBlank), "BOŞ", INK, 1);
        content.addView(stats);
        gap(content, 24);
        content.addView(text("SORU DAĞILIMI", 12, INK, true));
        gap(content, 12);
        for (int start = 0; start < 50; start += 10) {
            LinearLayout line = row();
            for (int j = start; j < Math.min(start + 10, 50); j++) {
                final int p = j;
                int chosen = answers[j];
                int color = chosen < 0 ? SOFT : chosen == exam.questions.get(j).correct ? GREEN : BAD;
                int fg = chosen < 0 ? MUTED : WHITE;
                TextView cell = text(String.valueOf(j + 1), 11, fg, true);
                cell.setGravity(Gravity.CENTER);
                cell.setBackground(rounded(color, 0, 6));
                cell.setOnClickListener(v -> { position = p; displayQuestion(true); });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(34), 1);
                lp.rightMargin = dp(3);
                line.addView(cell, lp);
            }
            content.addView(line);
            gap(content, 5);
        }
        gap(content, 13);
        content.addView(text("Yeşil: doğru   •   Kırmızı: yanlış   •   Gri: boş", 12, MUTED, false));
        gap(content, 25);
        TextView review = button("Cevapları incele", RED, WHITE, () -> { position = 0; displayQuestion(true); });
        content.addView(review);
        gap(content, 10);
        content.addView(button("Testi tekrar çöz", 0xFFF0F1F4, INK, () -> {
            new AlertDialog.Builder(this)
                .setTitle("Yeniden başla")
                .setMessage("Bu testin önceki sonucu kaydedildi. Yeni sınav başlatılsın mı? (Yeni sonuç, son sonuç kaydının yerini alır.)")
                .setPositiveButton("Başlat", (d, w) -> start(exam.id))
                .setNegativeButton("İptal", null)
                .show();
        }));
        gap(content, 10);
        content.addView(button("Tüm testler", PAPER, INK, this::home));
    }

    private void leaveQuestion() {
        if (screen == Screen.REVIEW) { showResult(); return; }
        if (screen == Screen.EXAM) {
            new AlertDialog.Builder(this)
                .setTitle("Sınavdan ayrıl")
                .setMessage("İşaretlediğin cevaplar kaydedilecek. Süre geri saymaya devam eder.")
                .setPositiveButton("Ana sayfa", (d, w) -> { saveSession(); home(); })
                .setNegativeButton("Sınava dön", null)
                .show();
        } else home();
    }

    @Override public void onBackPressed() {
        if (screen == Screen.EXAM || screen == Screen.REVIEW) leaveQuestion();
        else if (screen == Screen.RESULT) home();
        else super.onBackPressed();
    }

    @Override protected void onPause() {
        if (screen == Screen.EXAM) saveSession();
        stopClock();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (screen == Screen.EXAM && exam != null) {
            if (System.currentTimeMillis() >= deadline) finish(true);
            else if (tick == null) startClock();
        }
    }

    @Override protected void onDestroy() {
        stopClock();
        super.onDestroy();
    }
}

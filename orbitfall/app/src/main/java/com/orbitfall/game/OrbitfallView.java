package com.orbitfall.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public final class OrbitfallView extends View {
    private static final int MENU = 0, PLAYING = 1, PAUSED = 2, OVER = 3;
    private static final int INK = Color.rgb(8, 16, 34);
    private static final int MINT = Color.rgb(94, 234, 212);
    private static final int GOLD = Color.rgb(253, 230, 138);
    private static final int WHITE = Color.rgb(244, 248, 255);
    private static final int MUTED = Color.rgb(157, 177, 204);

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final ArrayList<Thing> things = new ArrayList<>();
    private final ArrayList<Spark> sparks = new ArrayList<>();
    private final Star[] stars = new Star[88];
    private final SharedPreferences prefs;

    private int state = MENU;
    private int best, score, fragments, shields = 1, chain;
    private float playerX, targetX, elapsed, invulnerable, spawnRock, spawnShard;
    private float scoreFraction, shake, menuPulse;
    private float worldW = 390f, worldH = 844f;
    private long lastFrame = 0L;
    private boolean dragging = false;

    public OrbitfallView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        prefs = context.getSharedPreferences("orbitfall_save", Context.MODE_PRIVATE);
        best = prefs.getInt("record", 0);
        text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        for (int i = 0; i < stars.length; i++) {
            stars[i] = new Star(random.nextFloat() * 420f,
                    random.nextFloat() * 1100f, 12f + random.nextFloat() * 52f,
                    0.6f + random.nextFloat() * 2.1f, 0.5f + random.nextFloat() * 1.4f);
        }
        playerX = targetX = 195f;
        setFocusable(true);
    }

    @Override
    protected void onDraw(Canvas raw) {
        super.onDraw(raw);
        float scale = Math.min(getWidth() / 390f, getHeight() / 720f);
        if (scale <= 0f) return;
        worldW = getWidth() / scale;
        worldH = getHeight() / scale;
        raw.save();
        raw.scale(scale, scale);
        float dt = 0.016f;
        long now = SystemClock.uptimeMillis();
        if (lastFrame != 0L) dt = Math.min(0.035f, Math.max(0.001f, (now - lastFrame) / 1000f));
        lastFrame = now;

        if (state == PLAYING) update(dt);
        menuPulse += dt;
        if (shake > 0f) shake = Math.max(0f, shake - dt * 28f);
        drawBackground(raw);
        if (state == MENU) drawMenu(raw);
        else if (state == PLAYING) drawGame(raw);
        else if (state == PAUSED) {
            drawGame(raw);
            drawPause(raw);
        } else {
            drawGame(raw);
            drawGameOver(raw);
        }
        raw.restore();
        postInvalidateDelayed(16L);
    }

    private void update(float dt) {
        elapsed += dt;
        if (invulnerable > 0f) invulnerable = Math.max(0f, invulnerable - dt);
        float speed = 185f + Math.min(165f, elapsed * 5.2f);
        playerX += (targetX - playerX) * Math.min(1f, dt * 12f);
        playerX = clamp(playerX, 27f, worldW - 27f);
        spawnRock -= dt;
        spawnShard -= dt;
        if (spawnRock <= 0f) {
            spawnRock = Math.max(0.43f, 1.04f - elapsed * 0.012f) * (0.78f + random.nextFloat() * 0.48f);
            float r = 13f + random.nextFloat() * 13f;
            things.add(new Thing(randomRange(24f + r, worldW - 24f - r), -r - 12f, r, 0,
                    speed * (0.76f + random.nextFloat() * 0.53f), random.nextFloat() * 6.28f,
                    random.nextInt(10000)));
        }
        if (spawnShard <= 0f) {
            spawnShard = 1.15f + random.nextFloat() * 0.85f;
            things.add(new Thing(randomRange(28f, worldW - 28f), -28f, 11f, 1,
                    speed * (0.83f + random.nextFloat() * 0.22f), 0f, random.nextInt(10000)));
        }
        scoreFraction += dt * 11f;
        if (scoreFraction >= 1f) {
            int add = (int) scoreFraction;
            score += add;
            scoreFraction -= add;
        }
        Iterator<Thing> it = things.iterator();
        while (it.hasNext()) {
            Thing t = it.next();
            t.y += t.vy * dt;
            t.angle += dt * (t.type == 0 ? 0.55f : 2.2f);
            if (t.type == 0) t.x += (float) Math.sin(elapsed * 1.2f + t.seed) * 13f * dt;
            float dx = t.x - playerX;
            float dy = t.y - worldH * 0.775f;
            float hit = t.r + (t.type == 0 ? 13f : 17f);
            if (dx * dx + dy * dy < hit * hit) {
                if (t.type == 1) {
                    fragments++;
                    chain++;
                    score += 45 + Math.min(45, chain * 2);
                    burst(t.x, t.y, GOLD, 12);
                    if (chain % 4 == 0 && shields < 3) {
                        shields++;
                        burst(playerX, worldH * 0.775f, MINT, 16);
                    }
                    it.remove();
                    continue;
                } else if (invulnerable <= 0f) {
                    if (shields > 0) {
                        shields--;
                        invulnerable = 1.45f;
                        shake = 9f;
                        burst(playerX, worldH * 0.775f, MINT, 24);
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                        it.remove();
                        continue;
                    } else {
                        shake = 15f;
                        burst(playerX, worldH * 0.775f, Color.rgb(255, 117, 117), 30);
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        finishRun();
                        break;
                    }
                }
            }
            if (t.y > worldH + 50f) it.remove();
        }
        for (Iterator<Spark> si = sparks.iterator(); si.hasNext();) {
            Spark s = si.next();
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            s.life -= dt;
            s.vy += 25f * dt;
            if (s.life <= 0f) si.remove();
        }
    }

    private void finishRun() {
        state = OVER;
        best = Math.max(best, score);
        prefs.edit().putInt("record", best).apply();
        dragging = false;
    }

    private void startRun() {
        things.clear();
        sparks.clear();
        elapsed = 0f;
        score = fragments = chain = 0;
        shields = 1;
        scoreFraction = 0f;
        invulnerable = 1f;
        spawnRock = 0.35f;
        spawnShard = 0.7f;
        playerX = targetX = worldW * 0.5f;
        state = PLAYING;
        lastFrame = SystemClock.uptimeMillis();
    }

    private void drawBackground(Canvas c) {
        bg.setShader(new LinearGradient(0f, 0f, worldW, worldH,
                new int[]{Color.rgb(8, 16, 34), Color.rgb(13, 26, 49), Color.rgb(24, 31, 57)},
                new float[]{0f, 0.58f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0f, 0f, worldW, worldH, bg);
        bg.setShader(null);

        float px = worldW * 0.79f, py = worldH * 0.22f;
        glow.setShader(new RadialGradient(px, py, 145f,
                new int[]{Color.argb(45, 94, 234, 212), Color.argb(0, 94, 234, 212)},
                null, Shader.TileMode.CLAMP));
        c.drawCircle(px, py, 145f, glow);
        glow.setShader(null);
        p.setColor(Color.rgb(20, 50, 72));
        p.setShader(new RadialGradient(px - 22f, py - 30f, 74f,
                new int[]{Color.rgb(46, 94, 112), Color.rgb(17, 40, 66), Color.rgb(11, 24, 45)},
                null, Shader.TileMode.CLAMP));
        c.drawCircle(px, py, 74f, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.3f);
        p.setColor(Color.argb(70, 153, 224, 222));
        c.save();
        c.rotate(-18f, px, py);
        c.drawOval(new RectF(px - 105f, py - 22f, px + 105f, py + 22f), p);
        c.restore();
        p.setStyle(Paint.Style.FILL);

        for (Star s : stars) {
            float sy = (s.y + menuPulse * s.speed * 9f) % (worldH + 4f);
            float twinkle = 0.55f + 0.45f * (float) Math.sin(menuPulse * s.twinkle + s.x);
            p.setColor(Color.argb((int) (65f + twinkle * 150f), 201, 225, 244));
            c.drawCircle(s.x % worldW, sy, s.size * 0.55f, p);
        }
        p.setColor(Color.argb(24, 94, 234, 212));
        p.setStrokeWidth(1f);
        for (int i = 0; i < 5; i++) {
            float yy = (worldH * 0.35f) + i * 54f + (menuPulse * 22f % 54f);
            c.drawLine(0f, yy, worldW, yy - 26f, p);
        }
    }

    private void drawMenu(Canvas c) {
        float mid = worldW * 0.5f;
        drawOrbitEmblem(c, mid, worldH * 0.205f, 52f);
        text(c, "НЕБЕСНАЯ АРКАДА", mid, worldH * 0.315f, 11f, MINT, Paint.Align.CENTER, true);
        text(c, "ОСКОЛКИ", mid, worldH * 0.385f, 37f, WHITE, Paint.Align.CENTER, true);
        text(c, "НЕБА", mid, worldH * 0.435f, 37f, WHITE, Paint.Align.CENTER, true);
        text(c, "Один корабль. Бесконечное поле.", mid, worldH * 0.485f, 13f, MUTED, Paint.Align.CENTER, false);
        text(c, "Собирай свет. Не стань пылью.", mid, worldH * 0.512f, 13f, MUTED, Paint.Align.CENTER, false);

        button(c, mid, worldH * 0.625f, 250f, 58f, "НАЧАТЬ ПОЛЁТ", true);
        text(c, "ВЕДИ КОРАБЛЬ ПАЛЬЦЕМ", mid, worldH * 0.715f, 10f, MINT, Paint.Align.CENTER, true);
        text(c, "ОСКОЛКИ ДАЮТ ОЧКИ И ЩИТ", mid, worldH * 0.743f, 10f, MUTED, Paint.Align.CENTER, false);
        drawRecord(c, mid, worldH * 0.82f);
        text(c, "БЕЗ ИНТЕРНЕТА  •  ПРОГРЕСС НА УСТРОЙСТВЕ", mid, worldH * 0.94f,
                8.5f, Color.argb(170, 157, 177, 204), Paint.Align.CENTER, false);
    }

    private void drawGame(Canvas c) {
        c.save();
        if (shake > 0f) c.translate(randomRange(-shake, shake), randomRange(-shake, shake));
        for (Thing t : things) drawThing(c, t);
        for (Spark s : sparks) {
            int alpha = (int) (255f * Math.min(1f, s.life / 0.32f));
            p.setColor(Color.argb(Math.max(0, alpha), Color.red(s.color), Color.green(s.color), Color.blue(s.color)));
            c.drawCircle(s.x, s.y, Math.max(0.8f, s.size * s.life * 2f), p);
        }
        drawShip(c, playerX, worldH * 0.775f);
        c.restore();

        rounded(c, 18f, 18f, 111f, 61f, 17f, Color.argb(195, 10, 23, 43), Color.argb(80, 118, 169, 195));
        text(c, "СЧЁТ", 31f, 40f, 9f, MUTED, Paint.Align.LEFT, true);
        text(c, String.valueOf(score), 31f, 64f, 22f, WHITE, Paint.Align.LEFT, true);
        rounded(c, worldW - 79f, 18f, worldW - 18f, 65f, 17f,
                Color.argb(195, 10, 23, 43), Color.argb(80, 118, 169, 195));
        p.setColor(MUTED);
        p.setStrokeWidth(2.1f);
        p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(worldW - 58f, 35f, worldW - 38f, 35f, p);
        c.drawLine(worldW - 58f, 42f, worldW - 38f, 42f, p);
        c.drawLine(worldW - 58f, 49f, worldW - 38f, 49f, p);
        text(c, "ПАУЗА", worldW - 48.5f, 64f, 7f, MUTED, Paint.Align.CENTER, true);

        rounded(c, 18f, 91f, 142f, 125f, 12f,
                Color.argb(115, 10, 23, 43), Color.argb(40, 118, 169, 195));
        text(c, "ОСКОЛКИ  " + fragments, 30f, 112f, 10f, GOLD, Paint.Align.LEFT, true);
        text(c, "ЩИТЫ  " + shields, 30f, 116f + 18f, 10f, MINT, Paint.Align.LEFT, true);

        float bx = worldW - 55f, by = worldH - 57f;
        glow.setShader(new RadialGradient(bx, by, 43f,
                new int[]{Color.argb(42, 94, 234, 212), Color.argb(0, 94, 234, 212)},
                null, Shader.TileMode.CLAMP));
        c.drawCircle(bx, by, 43f, glow);
        glow.setShader(null);
        p.setColor(Color.argb(205, 12, 33, 52));
        p.setStyle(Paint.Style.FILL);
        c.drawCircle(bx, by, 29f, p);
        p.setColor(shields > 0 ? MINT : Color.argb(95, 157, 177, 204));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.5f);
        c.drawCircle(bx, by, 29f, p);
        p.setStyle(Paint.Style.FILL);
        drawShieldIcon(c, bx, by, shields > 0 ? MINT : MUTED, 13f);
        text(c, "ЩИТ", bx, by + 45f, 8f, shields > 0 ? MINT : MUTED, Paint.Align.CENTER, true);
        if (invulnerable > 0f) {
            p.setColor(Color.argb(60, 94, 234, 212));
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2f);
            c.drawCircle(playerX, worldH * 0.775f, 30f + (1.45f - invulnerable) * 10f, p);
            p.setStyle(Paint.Style.FILL);
        }
    }

    private void drawGameOver(Canvas c) {
        p.setColor(Color.argb(212, 5, 11, 25));
        c.drawRect(0f, 0f, worldW, worldH, p);
        float mid = worldW * 0.5f;
        drawOrbitEmblem(c, mid, worldH * 0.26f, 38f);
        text(c, "СИГНАЛ ПОТЕРЯН", mid, worldH * 0.355f, 24f, WHITE, Paint.Align.CENTER, true);
        text(c, "Полёт окончен. Можно ещё раз.", mid, worldH * 0.397f, 12f, MUTED, Paint.Align.CENTER, false);
        rounded(c, 36f, worldH * 0.445f, worldW - 36f, worldH * 0.565f, 20f,
                Color.argb(190, 16, 34, 56), Color.argb(100, 94, 234, 212));
        text(c, "ИТОГОВЫЙ СЧЁТ", mid, worldH * 0.479f, 10f, MUTED, Paint.Align.CENTER, true);
        text(c, String.valueOf(score), mid, worldH * 0.526f, 34f, GOLD, Paint.Align.CENTER, true);
        text(c, "ОСКОЛКИ  " + fragments + "      РЕКОРД  " + best, mid, worldH * 0.601f,
                10f, MINT, Paint.Align.CENTER, true);
        button(c, mid, worldH * 0.68f, 248f, 56f, "ЕЩЁ ОДИН ПОЛЁТ", true);
        button(c, mid, worldH * 0.765f, 248f, 49f, "В МЕНЮ", false);
    }

    private void drawPause(Canvas c) {
        p.setColor(Color.argb(205, 5, 11, 25));
        c.drawRect(0f, 0f, worldW, worldH, p);
        float mid = worldW * 0.5f;
        drawOrbitEmblem(c, mid, worldH * 0.31f, 34f);
        text(c, "ПАУЗА", mid, worldH * 0.4f, 30f, WHITE, Paint.Align.CENTER, true);
        text(c, "Космос никуда не денется.", mid, worldH * 0.438f, 12f, MUTED, Paint.Align.CENTER, false);
        button(c, mid, worldH * 0.55f, 244f, 56f, "ПРОДОЛЖИТЬ", true);
        button(c, mid, worldH * 0.635f, 244f, 50f, "ЗАВЕРШИТЬ ПОЛЁТ", false);
    }

    private void drawThing(Canvas c, Thing t) {
        if (t.type == 1) {
            glow.setShader(new RadialGradient(t.x, t.y, t.r * 2.8f,
                    new int[]{Color.argb(95, 253, 230, 138), Color.argb(0, 253, 230, 138)},
                    null, Shader.TileMode.CLAMP));
            c.drawCircle(t.x, t.y, t.r * 2.8f, glow);
            glow.setShader(null);
            c.save();
            c.rotate((float) Math.toDegrees(t.angle), t.x, t.y);
            p.setColor(Color.rgb(255, 229, 137));
            Path path = new Path();
            path.moveTo(t.x, t.y - t.r * 1.35f);
            path.lineTo(t.x + t.r * 0.74f, t.y);
            path.lineTo(t.x, t.y + t.r * 1.35f);
            path.lineTo(t.x - t.r * 0.74f, t.y);
            path.close();
            c.drawPath(path, p);
            p.setColor(Color.WHITE);
            c.drawCircle(t.x - 1.8f, t.y - 2f, 2.1f, p);
            c.restore();
            return;
        }
        c.save();
        c.rotate((float) Math.toDegrees(t.angle), t.x, t.y);
        glow.setShader(new RadialGradient(t.x, t.y, t.r * 1.8f,
                new int[]{Color.argb(48, 241, 142, 107), Color.argb(0, 241, 142, 107)},
                null, Shader.TileMode.CLAMP));
        c.drawCircle(t.x, t.y, t.r * 1.8f, glow);
        glow.setShader(null);
        Path rock = new Path();
        int n = 9;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2.0 / n;
            float variance = 0.78f + 0.22f * (float) Math.sin(i * 8.7f + t.seed);
            float xx = t.x + (float) Math.cos(a) * t.r * variance;
            float yy = t.y + (float) Math.sin(a) * t.r * variance;
            if (i == 0) rock.moveTo(xx, yy); else rock.lineTo(xx, yy);
        }
        rock.close();
        p.setShader(new LinearGradient(t.x - t.r, t.y - t.r, t.x + t.r, t.y + t.r,
                new int[]{Color.rgb(155, 111, 108), Color.rgb(81, 79, 98), Color.rgb(42, 53, 75)},
                null, Shader.TileMode.CLAMP));
        c.drawPath(rock, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.3f);
        p.setColor(Color.argb(175, 218, 170, 153));
        c.drawPath(rock, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(110, 25, 37, 57));
        c.drawCircle(t.x - t.r * 0.23f, t.y + t.r * 0.11f, t.r * 0.22f, p);
        c.drawCircle(t.x + t.r * 0.24f, t.y - t.r * 0.25f, t.r * 0.12f, p);
        c.restore();
    }

    private void drawShip(Canvas c, float x, float y) {
        if (invulnerable > 0f && ((int) (elapsed * 16f) % 2 == 0)) return;
        glow.setShader(new RadialGradient(x, y + 1f, 48f,
                new int[]{Color.argb(70, 94, 234, 212), Color.argb(0, 94, 234, 212)},
                null, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, 48f, glow);
        glow.setShader(null);

        Path flame = new Path();
        flame.moveTo(x - 5f, y + 9f);
        flame.lineTo(x, y + 25f + random.nextFloat() * 9f);
        flame.lineTo(x + 5f, y + 9f);
        flame.close();
        p.setShader(new LinearGradient(x, y + 8f, x, y + 32f,
                new int[]{GOLD, MINT, Color.TRANSPARENT}, null, Shader.TileMode.CLAMP));
        c.drawPath(flame, p);
        p.setShader(null);

        Path ship = new Path();
        ship.moveTo(x, y - 26f);
        ship.cubicTo(x + 7f, y - 17f, x + 9f, y - 5f, x + 17f, y + 12f);
        ship.lineTo(x + 7f, y + 9f);
        ship.lineTo(x, y + 15f);
        ship.lineTo(x - 7f, y + 9f);
        ship.lineTo(x - 17f, y + 12f);
        ship.cubicTo(x - 9f, y - 5f, x - 7f, y - 17f, x, y - 26f);
        ship.close();
        p.setShader(new LinearGradient(x - 15f, y - 16f, x + 15f, y + 16f,
                new int[]{Color.rgb(247, 255, 255), MINT, Color.rgb(66, 144, 157)},
                null, Shader.TileMode.CLAMP));
        c.drawPath(ship, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.2f);
        p.setColor(Color.argb(210, 238, 255, 255));
        c.drawPath(ship, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(24, 53, 80));
        c.drawOval(new RectF(x - 4.3f, y - 15f, x + 4.3f, y - 3.5f), p);
        p.setColor(Color.argb(200, 255, 255, 255));
        c.drawCircle(x - 1.4f, y - 11f, 1.5f, p);
    }

    private void drawOrbitEmblem(Canvas c, float x, float y, float r) {
        glow.setShader(new RadialGradient(x, y, r * 1.8f,
                new int[]{Color.argb(60, 94, 234, 212), Color.argb(0, 94, 234, 212)},
                null, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, r * 1.8f, glow);
        glow.setShader(null);
        p.setColor(Color.argb(170, 94, 234, 212));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.5f);
        c.drawCircle(x, y, r, p);
        c.save();
        c.rotate(-30f, x, y);
        c.drawOval(new RectF(x - r * 1.43f, y - r * 0.36f, x + r * 1.43f, y + r * 0.36f), p);
        c.restore();
        p.setStyle(Paint.Style.FILL);
        p.setColor(GOLD);
        Path ship = new Path();
        ship.moveTo(x, y - r * 0.57f);
        ship.lineTo(x + r * 0.28f, y + r * 0.36f);
        ship.lineTo(x, y + r * 0.15f);
        ship.lineTo(x - r * 0.28f, y + r * 0.36f);
        ship.close();
        c.drawPath(ship, p);
        p.setColor(WHITE);
        c.drawCircle(x, y - r * 0.2f, Math.max(1.4f, r * 0.06f), p);
    }

    private void drawShieldIcon(Canvas c, float x, float y, int color, float size) {
        Path path = new Path();
        path.moveTo(x, y - size);
        path.lineTo(x + size * 0.78f, y - size * 0.62f);
        path.lineTo(x + size * 0.66f, y + size * 0.23f);
        path.lineTo(x, y + size);
        path.lineTo(x - size * 0.66f, y + size * 0.23f);
        path.lineTo(x - size * 0.78f, y - size * 0.62f);
        path.close();
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        c.drawPath(path, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(120, Color.red(color), Color.green(color), Color.blue(color)));
        c.drawCircle(x, y, size * 0.25f, p);
    }

    private void drawRecord(Canvas c, float x, float y) {
        p.setColor(Color.argb(130, 253, 230, 138));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.4f);
        c.drawCircle(x - 51f, y - 4f, 10f, p);
        p.setStyle(Paint.Style.FILL);
        text(c, "ЛУЧШИЙ РЕЗУЛЬТАТ  " + best, x + 4f, y, 10.5f, WHITE, Paint.Align.CENTER, true);
    }

    private void button(Canvas c, float cx, float cy, float width, float height, String label, boolean primary) {
        float l = cx - width * 0.5f, t = cy - height * 0.5f;
        if (primary) {
            glow.setShader(new RadialGradient(cx, cy, width * 0.65f,
                    new int[]{Color.argb(45, 94, 234, 212), Color.argb(0, 94, 234, 212)},
                    null, Shader.TileMode.CLAMP));
            c.drawRoundRect(l - 7f, t - 5f, l + width + 7f, t + height + 5f, 22f, 22f, glow);
            glow.setShader(null);
        }
        rounded(c, l, t, l + width, t + height, 17f,
                primary ? Color.rgb(94, 234, 212) : Color.argb(115, 21, 42, 62),
                primary ? Color.rgb(153, 255, 238) : Color.argb(125, 139, 172, 192));
        text(c, label, cx, cy + 5f, primary ? 12f : 11f,
                primary ? Color.rgb(7, 28, 43) : WHITE, Paint.Align.CENTER, true);
    }

    private void rounded(Canvas c, float l, float t, float r, float b, float radius, int fill, int stroke) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(fill);
        c.drawRoundRect(l, t, r, b, radius, radius, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1f);
        p.setColor(stroke);
        c.drawRoundRect(l, t, r, b, radius, radius, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void text(Canvas c, String value, float x, float y, float size, int color,
                      Paint.Align align, boolean bold) {
        text.setTextSize(size);
        text.setColor(color);
        text.setTextAlign(align);
        text.setTypeface(Typeface.create(bold ? "sans-serif" : "sans-serif-light",
                bold ? Typeface.BOLD : Typeface.NORMAL));
        text.setStyle(Paint.Style.FILL);
        c.drawText(value, x, y, text);
    }

    private void burst(float x, float y, int color, int count) {
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2.0;
            float speed = 38f + random.nextFloat() * 135f;
            sparks.add(new Spark(x, y, (float) Math.cos(a) * speed,
                    (float) Math.sin(a) * speed, 0.2f + random.nextFloat() * 0.48f,
                    1.4f + random.nextFloat() * 3.2f, color));
        }
    }

    private void useShield() {
        if (shields > 0 && invulnerable <= 0f && state == PLAYING) {
            shields--;
            invulnerable = 1.55f;
            burst(playerX, worldH * 0.775f, MINT, 22);
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float scale = Math.min(getWidth() / 390f, getHeight() / 720f);
        float x = e.getX() / scale, y = e.getY() / scale;
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            if (state == MENU) {
                startRun();
                return true;
            }
            if (state == OVER) {
                if (y > worldH * 0.735f) state = MENU;
                else startRun();
                return true;
            }
            if (state == PAUSED) {
                if (y < worldH * 0.60f) {
                    state = PLAYING;
                    lastFrame = SystemClock.uptimeMillis();
                } else {
                    state = MENU;
                }
                return true;
            }
            if (x > worldW - 88f && y > worldH - 120f) {
                useShield();
                return true;
            }
            if (x > worldW - 84f && y < 86f) {
                state = PAUSED;
                dragging = false;
                return true;
            }
            targetX = clamp(x, 25f, worldW - 25f);
            dragging = true;
            return true;
        }
        if (e.getAction() == MotionEvent.ACTION_MOVE && state == PLAYING && dragging) {
            targetX = clamp(x, 25f, worldW - 25f);
            return true;
        }
        if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
            dragging = false;
            return true;
        }
        return true;
    }

    private float randomRange(float min, float max) {
        return min + random.nextFloat() * Math.max(0f, max - min);
    }

    private float clamp(float n, float lo, float hi) {
        return Math.max(lo, Math.min(hi, n));
    }

    private static final class Thing {
        float x, y, r, vy, angle;
        int type, seed;
        Thing(float x, float y, float r, int type, float vy, float angle, int seed) {
            this.x = x; this.y = y; this.r = r; this.type = type;
            this.vy = vy; this.angle = angle; this.seed = seed;
        }
    }

    private static final class Spark {
        float x, y, vx, vy, life, size;
        int color;
        Spark(float x, float y, float vx, float vy, float life, float size, int color) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.life = life; this.size = size; this.color = color;
        }
    }

    private static final class Star {
        float x, y, size, speed, twinkle;
        Star(float x, float y, float size, float speed, float twinkle) {
            this.x = x; this.y = y; this.size = size; this.speed = speed; this.twinkle = twinkle;
        }
    }
}

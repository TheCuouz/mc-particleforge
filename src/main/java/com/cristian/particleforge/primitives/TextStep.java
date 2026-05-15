package com.cristian.particleforge.primitives;

import com.cristian.particleforge.engine.BudgetManager;
import com.cristian.particleforge.model.EffectContext;
import com.cristian.particleforge.model.EffectStep;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renders a string in particles using {@link BlockFont}. Text reads
 * left-to-right along the rotated X-axis, top-to-bottom in Y.
 *
 * <h2>Params</h2>
 * <ul>
 *   <li>{@code particle} (string) — default {@code END_ROD}.</li>
 *   <li>{@code count} (int) — per pixel; default {@code 1}.</li>
 *   <li>{@code text} (string) — default {@code "HELLO"}.</li>
 *   <li>{@code scale} (double) — per-pixel size; default {@code 0.2}.</li>
 *   <li>{@code yaw-deg} (double) — facing rotation; default {@code 0}.</li>
 *   <li>{@code char-spacing} (double) — extra blocks between glyphs; default {@code scale * 1}.</li>
 * </ul>
 *
 * <h2>LOD</h2>
 * Scales {@code count} per pixel. The pixel layout itself stays so the text
 * remains readable. Bucket {@link BudgetManager#BUCKET_CULL} → no emit.
 */
public final class TextStep implements EffectStep {

    private final String id;
    private final int durationTicks;
    private final Particle particle;
    private final int count;
    private final String text;
    private final double scale;
    private final double yawDeg;
    private final double charSpacing;
    private final double speed;
    private final double spread;

    private final List<double[]> cachedOffsets;

    public TextStep(String id, int durationTicks, Map<String, Object> params) {
        this.id = id;
        this.durationTicks = durationTicks;
        this.particle = ParamUtil.particle(params, "particle", Particle.END_ROD);
        this.count    = ParamUtil.intVal(params, "count", 1);
        this.text     = ParamUtil.stringVal(params, "text", "HELLO");
        this.scale    = ParamUtil.doubleVal(params, "scale", 0.2);
        this.yawDeg   = ParamUtil.doubleVal(params, "yaw-deg", 0.0);
        this.charSpacing = ParamUtil.doubleVal(params, "char-spacing", this.scale);
        this.speed    = ParamUtil.doubleVal(params, "speed", 0.0);
        this.spread   = ParamUtil.doubleVal(params, "spread", 0.0);
        this.cachedOffsets = computePixelOffsets();
    }

    @Override public String id() { return id; }
    @Override public int durationTicks() { return durationTicks; }

    @Override
    public void tick(EffectContext ctx, int tickInStep) {
        if (ctx.lodBucket() >= BudgetManager.BUCKET_CULL || ctx.lodMultiplier() <= 0.0) return;
        Location origin = ctx.origin();
        if (origin == null) return;
        int eff = effectiveCount(ctx.lodMultiplier());
        if (eff <= 0) return;
        for (double[] off : cachedOffsets) {
            Location at = origin.clone().add(off[0], off[1], off[2]);
            SpawnUtil.spawn(at, ctx, particle, eff, spread, speed);
        }
    }

    /** Package-private for tests. */
    int effectiveCount(double lodMul) {
        if (lodMul <= 0.0) return 0;
        return Math.max(1, (int) Math.round(count * lodMul));
    }

    /** Package-private for tests. Returns all (dx,dy,dz) offsets for lit pixels. */
    List<double[]> pixelOffsets() {
        return cachedOffsets;
    }

    private List<double[]> computePixelOffsets() {
        List<double[]> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        double glyphAdvance = BlockFont.COLS * scale + charSpacing;
        for (int i = 0; i < text.length(); i++) {
            boolean[][] glyph = BlockFont.glyph(text.charAt(i));
            double xOriginGlyph = i * glyphAdvance;
            for (int r = 0; r < BlockFont.ROWS; r++) {
                for (int c = 0; c < BlockFont.COLS; c++) {
                    if (!glyph[r][c]) continue;
                    double dx = xOriginGlyph + c * scale;
                    double dy = (BlockFont.ROWS - 1 - r) * scale;
                    double dz = 0.0;
                    out.add(Geometry.rotate(new double[]{dx, dy, dz}, yawDeg, 0.0));
                }
            }
        }
        return out;
    }
}

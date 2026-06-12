package com.bloodpressure.app.ui.advice;

import com.bloodpressure.app.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HealthAdviceContent {

    public static final String TYPE_PINGHE = "平和质";
    public static final String TYPE_YINXU = "阴虚质";
    public static final String TYPE_TANSHI = "痰湿质";
    public static final String TYPE_QIYU = "气郁质";

    /**
     * 分类结果，包含体质类型、置信度（百分比）和显示文本。
     */
    public static class ConstitutionResult {
        public final String type;
        public final int confidence; // 0-100
        public final String displayText;

        public ConstitutionResult(String type, int confidence, String displayText) {
            this.type = type;
            this.confidence = confidence;
            this.displayText = displayText;
        }
    }

    /**
     * 根据问卷得分进行体质分类，返回置信度最高的 1~2 种体质。
     * 当第二名与第一名得分差在 20% 以内时，同时返回两种体质。
     */
    public static List<ConstitutionResult> classifyTop(int scorePinghe, int scoreYinxu,
                                                       int scoreTanshi, int scoreQiyu) {
        int total = scorePinghe + scoreYinxu + scoreTanshi + scoreQiyu;
        if (total == 0) total = 1;

        List<ConstitutionResult> all = new ArrayList<>();
        all.add(new ConstitutionResult(TYPE_PINGHE, Math.round(scorePinghe * 100f / total),
                TYPE_PINGHE + " " + Math.round(scorePinghe * 100f / total) + "%"));
        all.add(new ConstitutionResult(TYPE_YINXU, Math.round(scoreYinxu * 100f / total),
                TYPE_YINXU + " " + Math.round(scoreYinxu * 100f / total) + "%"));
        all.add(new ConstitutionResult(TYPE_TANSHI, Math.round(scoreTanshi * 100f / total),
                TYPE_TANSHI + " " + Math.round(scoreTanshi * 100f / total) + "%"));
        all.add(new ConstitutionResult(TYPE_QIYU, Math.round(scoreQiyu * 100f / total),
                TYPE_QIYU + " " + Math.round(scoreQiyu * 100f / total) + "%"));

        Collections.sort(all, (a, b) -> b.confidence - a.confidence);

        List<ConstitutionResult> results = new ArrayList<>();
        results.add(all.get(0));

        if (all.size() > 1 && all.get(1).confidence >= all.get(0).confidence - 20) {
            results.add(all.get(1));
        }

        return results;
    }

    /**
     * 兼容旧逻辑：返回置信度最高的单一类型。
     */
    public static String classify(int scorePinghe, int scoreYinxu, int scoreTanshi, int scoreQiyu) {
        return classifyTop(scorePinghe, scoreYinxu, scoreTanshi, scoreQiyu).get(0).type;
    }

    /**
     * 返回体质类型对应的颜色资源 ID。
     */
    public static int getColorRes(String constitutionType) {
        switch (constitutionType) {
            case TYPE_YINXU:
                return R.color.type_yinxu;
            case TYPE_TANSHI:
                return R.color.type_tanshi;
            case TYPE_QIYU:
                return R.color.type_qiyu;
            default:
                return R.color.type_pinghe;
        }
    }

    // ── 单体质建议 ──────────────────────────────────────────────

    private static String dietaryPinghe() {
        return "您的体质状态良好，建议继续保持规律的饮食习惯。日常可食用山药薏苡仁粥来健脾养胃，"
                + "做到饮食均衡、食物多样，五谷杂粮搭配新鲜蔬果，适量摄入优质蛋白质。"
                + "注意控制钠盐摄入，避免暴饮暴食，细嚼慢咽有助于消化吸收。";
    }

    private static String dietaryYinxu() {
        return "阴虚体质宜滋阴润燥。推荐早餐食用枸杞粥，取枸杞15克与粳米同煮，"
                + "可滋补肝肾、明目润肺。日常可饮用银耳藕粉羹，银耳富含胶质，"
                + "有滋阴生津之效。同时应避免辛辣燥热食物，少食煎炸食品，"
                + "忌烟酒及浓茶，以免耗伤阴液。";
    }

    private static String dietaryTanshi() {
        return "痰湿体质宜健脾利湿、化痰降浊。推荐薏米赤小豆粥，取薏米30克、"
                + "赤小豆20克熬煮成粥，可健脾祛湿。日常可饮用橘皮山楂乌龙茶，"
                + "橘皮理气化痰，山楂消食降脂，乌龙茶利水渗湿。"
                + "忌油腻、甜食及冰冷饮品，少食肥甘厚味。";
    }

    private static String dietaryQiyu() {
        return "气郁体质宜疏肝理气、解郁安神。推荐玫瑰花茶，取干玫瑰花5朵沸水冲泡，"
                + "可疏肝解郁、活血调经。日常可食用山楂粥，取山楂15克与粳米同煮，"
                + "能消食化积、活血散瘀。同时应保持心情舒畅，避免过量饮用咖啡和浓茶。";
    }

    private static String acupressurePinghe() {
        return "您的气血运行较为顺畅，日常可常按足三里穴（外膝眼下四横指、胫骨前嵴外一横指处），"
                + "每次按揉3-5分钟，可健脾和胃、扶正培元。"
                + "配合按揉三阴交穴（内踝尖上四横指、胫骨后缘处），有助于调补肝脾肾三经，"
                + "维持气血平衡，增强体质。";
    }

    private static String acupressureYinxu() {
        return "阴虚体质可常按太溪穴（内踝后方、跟腱前缘凹陷处），每次按揉3-5分钟，"
                + "此穴为肾经原穴，有滋阴益肾之效。"
                + "配合按揉太冲穴（足背第一、二跖骨结合部之前凹陷处），可平肝潜阳。"
                + "再按揉三阴交穴（内踝尖上四横指处），三穴合用，有助于滋阴降火、调理脏腑。";
    }

    private static String acupressureTanshi() {
        return "痰湿体质可常按丰隆穴（外踝尖上八横指、胫骨前嵴外二横指处），"
                + "此穴为化痰要穴，每次按揉3-5分钟。"
                + "配合按揉足三里穴（外膝眼下四横指处），可健脾和胃、助运化湿。"
                + "再按揉中脘穴（脐上四横指处），能和胃健脾、降逆利水，三穴合用有助于化痰祛湿。";
    }

    private static String acupressureQiyu() {
        return "气郁体质可常按太冲穴（足背第一、二跖骨结合部之前凹陷处），"
                + "此穴为疏肝理气要穴，每次按揉3-5分钟。"
                + "配合按揉内关穴（腕横纹上二横指、两筋之间处），可宁心安神、理气止痛。"
                + "再按揉膻中穴（两乳头连线中点处），能宽胸理气、活血通络，三穴合用有助于疏肝解郁。";
    }

    private static String earPinghe() {
        return "推荐耳穴压豆取穴：降压沟、神门。"
                + "降压沟位于耳背上方，有辅助调节血压的作用；"
                + "神门位于耳甲艇内，可镇静安神。"
                + "建议每日按压3-5次，每次每穴按压1-2分钟，以轻微酸胀为度。";
    }

    private static String earYinxu() {
        return "推荐耳穴压豆取穴：降压沟、神门、肾（CO10）。"
                + "降压沟位于耳背上方，辅助调节血压；"
                + "神门可镇静安神；肾穴（CO10）位于耳甲艇内，有滋阴补肾之效。"
                + "建议每日按压3-5次，每次每穴按压1-2分钟，以轻微酸胀为度。";
    }

    private static String earTanshi() {
        return "推荐耳穴压豆取穴：降压沟、神门、脾。"
                + "降压沟辅助调节血压；神门可镇静安神；"
                + "脾穴位于耳甲腔内，有健脾化湿之效。"
                + "建议每日按压3-5次，每次每穴按压1-2分钟，以轻微酸胀为度。";
    }

    private static String earQiyu() {
        return "推荐耳穴压豆取穴：降压沟、神门、肝（CO12）。"
                + "降压沟辅助调节血压；神门可镇静安神；"
                + "肝穴（CO12）位于耳甲艇内，有疏肝理气之效。"
                + "建议每日按压3-5次，每次每穴按压1-2分钟，以轻微酸胀为度。";
    }

    // ── 公开方法：根据体质返回单条或合并建议 ──────────────────

    /**
     * 返回饮食调理建议。支持单体质和双体质。
     */
    public static String getDietaryAdvice(String type1, String type2) {
        String part1 = dietaryFor(type1);
        if (type2 == null) return part1;
        return part1 + "\n\n" + dietaryFor(type2);
    }

    /**
     * 返回穴位按摩建议。支持单体质和双体质。
     */
    public static String getAcupressureAdvice(String type1, String type2) {
        String part1 = acupressureFor(type1);
        if (type2 == null) return part1;
        return part1 + "\n\n" + acupressureFor(type2);
    }

    /**
     * 返回耳穴压豆建议。支持单体质和双体质。
     */
    public static String getEarAcupressureAdvice(String type1, String type2) {
        String part1 = earFor(type1);
        if (type2 == null) return part1;
        return part1 + "\n\n" + earFor(type2);
    }

    /**
     * 兼容旧接口：生成完整健康建议，格式：[饮食调理, 穴位按摩, 耳穴压豆]
     */
    public static String[] getAdvice(String constitutionType) {
        return new String[]{
                getDietaryAdvice(constitutionType, null),
                getAcupressureAdvice(constitutionType, null),
                getEarAcupressureAdvice(constitutionType, null)
        };
    }

    // ── 内部分发 ──────────────────────────────────────────────

    private static String dietaryFor(String type) {
        switch (type) {
            case TYPE_YINXU:  return dietaryYinxu();
            case TYPE_TANSHI: return dietaryTanshi();
            case TYPE_QIYU:   return dietaryQiyu();
            default:          return dietaryPinghe();
        }
    }

    private static String acupressureFor(String type) {
        switch (type) {
            case TYPE_YINXU:  return acupressureYinxu();
            case TYPE_TANSHI: return acupressureTanshi();
            case TYPE_QIYU:   return acupressureQiyu();
            default:          return acupressurePinghe();
        }
    }

    private static String earFor(String type) {
        switch (type) {
            case TYPE_YINXU:  return earYinxu();
            case TYPE_TANSHI: return earTanshi();
            case TYPE_QIYU:   return earQiyu();
            default:          return earPinghe();
        }
    }

    // ── 耳穴图片 ──────────────────────────────────────────────

    /**
     * 返回体质对应的耳前穴位图 drawable 资源 ID。
     */
    public static int getEarFrontImageRes(String constitutionType) {
        switch (constitutionType) {
            case TYPE_TANSHI:
                return R.drawable.ear_tanshi;
            case TYPE_YINXU:
                return R.drawable.ear_yinxu;
            case TYPE_QIYU:
                return R.drawable.ear_qiyu;
            default:
                return R.drawable.ear_pinghe;
        }
    }

    /**
     * 返回耳背穴位图 drawable 资源 ID（所有体质通用）。
     */
    public static int getEarBackImageRes() {
        return R.drawable.ear_back_all;
    }

    /**
     * 返回体质对应的饮食调理配图 drawable 资源 ID，无图返回 0。
     */
    public static int getDietaryImageRes(String constitutionType) {
        switch (constitutionType) {
            case TYPE_TANSHI:
                return R.drawable.diet_tanshi;
            case TYPE_YINXU:
                return R.drawable.diet_yinxu;
            case TYPE_QIYU:
                return R.drawable.diet_qiyu;
            default:
                return R.drawable.diet_pinghe;
        }
    }
}

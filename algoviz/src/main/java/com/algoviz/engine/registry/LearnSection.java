package com.algoviz.engine.registry;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.AlgorithmRegistry;

import java.util.List;

/** 「学习」域：深度学习 + 机器学习（固定剧本）。 */
public final class LearnSection implements AlgorithmSection {

    @Override
    public List<AlgorithmRegistry.Entry> entries() {
        return List.of(
                new AlgorithmRegistry.Entry("深度学习", null, new com.algoviz.engine.dl.CnnDemo()),
                new AlgorithmRegistry.Entry("深度学习", null, new com.algoviz.engine.dl.RnnDemo()),
                new AlgorithmRegistry.Entry("深度学习", null, new com.algoviz.engine.dl.GnnDemo()),
                new AlgorithmRegistry.Entry("深度学习", null, new com.algoviz.engine.dl.GanDemo()),
                new AlgorithmRegistry.Entry("深度学习", null, new com.algoviz.engine.dl.AttentionDemo()),
                new AlgorithmRegistry.Entry("深度学习", null, new com.algoviz.engine.dl.LstmDemo()),
                new AlgorithmRegistry.Entry("深度学习", null, new com.algoviz.engine.dl.BackpropDemo()),
                new AlgorithmRegistry.Entry("深度学习", null, new com.algoviz.engine.dl.Word2VecDemo()),
                new AlgorithmRegistry.Entry("深度学习", null, new com.algoviz.engine.dl.ResNetDemo()),
                new AlgorithmRegistry.Entry("深度学习", null, new com.algoviz.engine.dl.DropoutDemo()),
                new AlgorithmRegistry.Entry("深度学习", null, new com.algoviz.engine.dl.BatchNormDemo()),
                new AlgorithmRegistry.Entry("深度学习", null, new com.algoviz.engine.dl.KMeansDemo()),
                new AlgorithmRegistry.Entry("深度学习", null, new com.algoviz.engine.dl.GradientDescentDemo()),
                new AlgorithmRegistry.Entry("机器学习", null, new com.algoviz.engine.ml.QlearnDemo()),
                new AlgorithmRegistry.Entry("机器学习", null, new com.algoviz.engine.ml.DtreeDemo()),
                new AlgorithmRegistry.Entry("机器学习", null, new com.algoviz.engine.ml.NaiveBayesDemo())
        );
    }

    @Override
    public Algorithm variantOf(String id, int n, java.util.Random r) {
        return null;   // 固定剧本域：无「换一批数据」
    }
}

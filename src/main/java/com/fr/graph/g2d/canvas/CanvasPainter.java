package com.fr.graph.g2d.canvas;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import com.fr.general.IOUtils;
import com.fr.log.FineLoggerFactory;
import com.fr.stable.StringUtils;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.InputStream;

/**
 * @author richie
 * @version 10.0
 * Created by richie on 2019-05-21
 */
public class CanvasPainter implements Closeable {

    /**
     * 生成用于构建Canvas画板的构建器
     * @return 构建器
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * 生成用于构建Canvas画板的构建器，默认加载了用于预处理的内置JS
     * @return 构建器
     */
    public static Builder newDefaultBuilder() {
        return new Builder().prepare("/com/fr/graph/g2d/canvas/js/fx-adapter.js");
    }

    private V8 v8 = V8.createV8Runtime();
    private StringBuilder sb = new StringBuilder();
    private CanvasAdapter canvas = new CanvasAdapter();

    private CanvasPainter() {
        register(v8);
    }

    public static void register(final V8 v8) {
        V8Function constructor = new V8Function(v8, new JavaCallback() {
            @Override
            public Object invoke(V8Object receiver, V8Array parameters) {
                if (parameters.length() == 2) {
                    int w = parameters.getInteger(0);
                    int h = parameters.getInteger(1);
                    return new V8Canvas(v8, new CanvasAdapter(w, h));
                } else {
                    return new V8Canvas(v8, new CanvasAdapter());
                }
            }
        });
        v8.add("Canvas", constructor);
        constructor.release();
    }

    public BufferedImage paint() {
        execute();
        return canvas.getCanvas();
    }

    private void initCanvas() {
        V8Canvas v8Canvas = new V8Canvas(v8, canvas);
        v8.add("canvas", v8Canvas);
        v8Canvas.release();
    }

    private void execute(String script) {
        v8.executeVoidScript(script);
    }

    private void add(String script) {
        sb.append(script);
        sb.append(";");
    }

    private void execute() {
        v8.executeVoidScript(sb.toString());
        sb.setLength(0);
    }

    public void close() {
        v8.release(true);
    }


    public static class Builder {

        private CanvasPainter painter = new CanvasPainter();

        public Builder prepare(String filePath) {
            String content = readFileBody(filePath);
            painter.execute(content);
            painter.initCanvas();
            return this;
        }

        public Builder loadAndExecute(String... filePath) {
            for (String path : filePath) {
                String content = readFileBody(path);
                painter.execute(content);
            }
            return this;
        }

        public Builder loadFile(String... filePath) {
            for (String path : filePath) {
                painter.add(readFileBody(path));
            }
            return this;
        }

        public Builder loadText(String... strings) {
            for (String text : strings) {
                painter.add(text);
            }
            return this;
        }

        private String readFileBody(String filePath) {
            InputStream in = CanvasPainter.Builder.class.getResourceAsStream(filePath);
            String script = StringUtils.EMPTY;
            try {
                script = IOUtils.inputStream2String(in);
            }  catch (Exception e) {
                FineLoggerFactory.getLogger().error(e.getMessage(), e);
            }
            return script;
        }

        public CanvasPainter build() {
            return painter;
        }
    }
}
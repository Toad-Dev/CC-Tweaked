/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration;

import com.mojang.blaze3d.vertex.VertexFormat;
import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.client.render.text.DirectFixedWidthFontRenderer;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisTextVertexSink;
import net.minecraftforge.fml.ModList;

import java.nio.ByteBuffer;
import java.util.function.IntFunction;

public class ShaderMod
{
    public static final ShaderMod INSTANCE
        = ModList.get().isLoaded( "oculus" ) ? new IrisImpl()
        : new ShaderMod();

    public boolean isShaderMod()
    {
        return Optifine.isLoaded();
    }

    public boolean isRenderingShadowPass()
    {
        return false;
    }

    public DirectFixedWidthFontRenderer.QuadEmitter getQuadEmitter( int vertexCount, IntFunction<ByteBuffer> makeBuffer )
    {
        return new DirectFixedWidthFontRenderer.ByteBufferEmitter(
            makeBuffer.apply( RenderTypes.TERMINAL.format().getVertexSize() * vertexCount * 4 )
        );
    }

    private static final class IrisImpl extends ShaderMod
    {
        @Override
        public boolean isRenderingShadowPass()
        {
            return IrisApi.getInstance().isRenderingShadowPass();
        }

        @Override
        public boolean isShaderMod()
        {
            return true;
        }

        @Override
        public DirectFixedWidthFontRenderer.QuadEmitter getQuadEmitter( int vertexCount, IntFunction<ByteBuffer> makeBuffer )
        {
            return IrisApi.getInstance().getMinorApiRevision() >= 1
                ? new IrisQuadEmitter( vertexCount, makeBuffer )
                : super.getQuadEmitter( vertexCount, makeBuffer );
        }

        private static final class IrisQuadEmitter implements DirectFixedWidthFontRenderer.QuadEmitter
        {
            private final IrisTextVertexSink sink;

            private IrisQuadEmitter( int vertexCount, IntFunction<ByteBuffer> makeBuffer )
            {
                sink = IrisApi.getInstance().createTextVertexSink( vertexCount, makeBuffer );
            }

            @Override
            public VertexFormat format()
            {
                return sink.getUnderlyingVertexFormat();
            }

            @Override
            public ByteBuffer buffer()
            {
                return sink.getUnderlyingByteBuffer();
            }

            @Override
            public void quad( float x1, float y1, float x2, float y2, float z, byte[] rgba, float u1, float v1, float u2, float v2 )
            {
                sink.quad( x1, y1, x2, y2, z, pack( rgba[0], rgba[1], rgba[2], rgba[3] ), u1, v1, u2, v2, RenderTypes.FULL_BRIGHT_LIGHTMAP );
            }

            private static int pack( int r, int g, int b, int a )
            {
                return (a & 255) << 24 | (b & 255) << 16 | (g & 255) << 8 | r & 255;
            }
        }
    }
}
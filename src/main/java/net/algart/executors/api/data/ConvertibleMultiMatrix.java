/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.algart.executors.api.data;

import net.algart.arrays.*;
import net.algart.multimatrix.MultiMatrix;

import java.nio.ByteBuffer;
import java.util.Objects;

public class ConvertibleMultiMatrix extends SMat.Convertible {
    final MultiMatrix multiMatrix;
    final SMat.ChannelOrder channelOrder;

    public ConvertibleMultiMatrix(MultiMatrix multiMatrix) {
        this(multiMatrix, SMat.ChannelOrder.STANDARD);
    }

    ConvertibleMultiMatrix(MultiMatrix multiMatrix, SMat.ChannelOrder channelOrder) {
        this.multiMatrix = Objects.requireNonNull(multiMatrix, "Null multiMatrix");
        this.channelOrder = Objects.requireNonNull(channelOrder, "Null channelOrder");
    }


    @Override
    public SMat.Convertible copy() {
        return this;
    }

    @Override
    public SMat.Convertible copyToMemoryAndDisposePrevious() {
        return this;
    }

    @Override
    public ByteBuffer toByteBuffer(SMat thisMatrix) {
//        System.out.println("!!! conversion of " + multiMatrix);
        final long[] newDimensions = SMat.addFirstElement(multiMatrix.numberOfChannels(), multiMatrix.dimensions());
        if (multiMatrix.numberOfChannels() == 1) {
            return toByteBuffer(Matrices.matrix(multiMatrix.intensityChannel().array(), newDimensions));
        }
        long t1 = System.nanoTime();
        final Matrix<? extends UpdatablePArray> packedChannels = BufferMemoryModel.getInstance().newMatrix(
                UpdatablePArray.class,
                multiMatrix.elementType(),
                newDimensions);
        long t2 = System.nanoTime();
        Matrices.interleave(null,
                packedChannels,
                channelOrder == SMat.ChannelOrder.ORDER_IN_PACKED_BYTE_BUFFER ?
                        multiMatrix.allChannels() :
                        multiMatrix.allChannelsInBGRAOrder());
        long t3 = System.nanoTime();
//        System.out.printf("!!!!!! %.3f allocate + %.3f ms pack%n", (t2 - t1) * 1e-6, (t3 - t2) * 1e-6);
        return toByteBuffer(packedChannels);
    }

    public byte[] toByteArray(SMat thisMatrix) {
        if (thisMatrix.getDepth().elementType() != byte.class) {
            return super.toByteArray(thisMatrix);
        }
//        System.out.println("!!! conversion of " + multiMatrix);
        if (multiMatrix.numberOfChannels() == 1) {
            final PArray array = multiMatrix.intensityChannel().array();
            if (array instanceof DirectAccessible
                    && ((DirectAccessible) array).hasJavaArray()
                    && ((DirectAccessible) array).javaArrayOffset() == 0
                    && ((DirectAccessible) array).javaArrayLength() == array.length()) {
                return (byte[]) ((DirectAccessible) array).javaArray();
            }
            return (byte[]) Arrays.toJavaArray(array);
        }
        long t1 = System.nanoTime();
        final long size = Arrays.longMul(multiMatrix.numberOfChannels(), multiMatrix.size());
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large matrix: " + multiMatrix);
        }
        byte[] result = new byte[(int) size];
        final Matrix<? extends UpdatablePArray> packedChannels =  SimpleMemoryModel.asMatrix(
                result,
                SMat.addFirstElement(multiMatrix.numberOfChannels(), multiMatrix.dimensions()));
        long t2 = System.nanoTime();
        Matrices.interleave(
                null,
                packedChannels,
                channelOrder == SMat.ChannelOrder.ORDER_IN_PACKED_BYTE_BUFFER ?
                        multiMatrix.allChannels() :
                        multiMatrix.allChannelsInBGRAOrder());
        long t3 = System.nanoTime();
//        System.out.printf("!!!!!! %.3f + %.3f ms%n", (t2 - t1) * 1e-6, (t3 - t2) * 1e-6);
        return result;
    }

    @Override
    public void dispose() {
    }

    @Override
    public String toString() {
        return "reference to " + multiMatrix;
    }

    public static ByteBuffer toByteBuffer(Matrix<? extends PArray> packedChannels) {
        Objects.requireNonNull(packedChannels, "Null BGR[A] matrix");
        final int dimCount = packedChannels.dimCount();
        if (dimCount < 2) {
            throw new IllegalArgumentException("Packed BGR[A] matrix cannot be 1-dimensional: " + packedChannels
                    + " (the 1st dimension is used to store channels)");
        }
        // 2-dimensional matrix must be grayscale
        if (packedChannels.dim(0) > MultiMatrix.MAX_NUMBER_OF_CHANNELS) {
            throw new IllegalArgumentException("Number of channels cannot be >"
                    + MultiMatrix.MAX_NUMBER_OF_CHANNELS + ": " + packedChannels);
        }
        Array array = packedChannels.array();
        if (!(BufferMemoryModel.isBufferArray(array) && BufferMemoryModel.getBufferOffset(array) == 0)) {
            // Important: if offset != 0, it is a subarray, and we must create its copy before storing in SMat!
            array = array.updatableClone(BufferMemoryModel.getInstance());
        }
        assert BufferMemoryModel.isBufferArray(array);
        return BufferMemoryModel.getByteBuffer(array);
    }

    @Override
    MultiMatrix getCachedMultiMatrix(
            SMat thisMat,
            boolean autoConvertUnsupportedDepth,
            SMat.ChannelOrder channelOrder) {
        if (channelOrder == this.channelOrder) {
//            System.out.println("!!! quick returning " + this);
            return multiMatrix;
        }
        return super.getCachedMultiMatrix(thisMat, autoConvertUnsupportedDepth, channelOrder);
    }
}

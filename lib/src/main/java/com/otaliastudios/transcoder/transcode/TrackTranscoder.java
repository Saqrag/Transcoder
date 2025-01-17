/*
 * Copyright (C) 2014 Yuya Tanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.otaliastudios.transcoder.transcode;

import android.media.MediaFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface TrackTranscoder {

    void setUp(@NonNull MediaFormat desiredOutputFormat);

    /**
     * Perform transcoding if output is available in any step of it.
     * It assumes muxer has been started, so you should call muxer.start() first.
     *
     * @return true if data moved in pipeline.
     */
    boolean transcode();

    /**
     * Get presentation time of last sample taken from encoder.
     * This presentation time should not be affected by {@link com.otaliastudios.transcoder.time.TimeInterpolator}s.
     *
     * @return Presentation time in microseconds. Return value is undefined if finished writing.
     */
    long getLastPresentationTime();

    boolean isFinished();

    void release();
}

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
package com.otaliastudios.transcoder.engine;

import com.otaliastudios.transcoder.strategy.OutputStrategy;
import com.otaliastudios.transcoder.validator.Validator;

import androidx.annotation.Nullable;

/**
 * An exception thrown internally when a {@link Validator}
 * returns false. Not to be used.
 */
public class ValidatorException extends RuntimeException {
    ValidatorException(@Nullable String detailMessage) {
        super(detailMessage);
    }
}

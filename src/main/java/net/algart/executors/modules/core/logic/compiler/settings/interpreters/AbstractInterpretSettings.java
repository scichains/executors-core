/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.core.logic.compiler.settings.interpreters;

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.modules.core.logic.compiler.settings.UseSettings;
import net.algart.executors.modules.core.logic.compiler.settings.model.SettingsCombiner;

public abstract class AbstractInterpretSettings extends Executor implements ReadOnlyExecutionInput {
    private volatile SettingsCombiner settingsCombiner = null;

    public SettingsCombiner settingsCombiner() {
        SettingsCombiner settingsCombiner = this.settingsCombiner;
        if (settingsCombiner == null) {
            settingsCombiner = UseSettings.settingsCombinerLoader().reqRegisteredWorker(getExecutorId());
            this.settingsCombiner = settingsCombiner.clone();
            // - the order is important for multithreading: local settingsCombiner is assigned first,
            // this.settingsCombiner is assigned to it;
            // cloning is necessary because SettingsCombiner class can be little customized
        }
        return settingsCombiner;
    }

    void setSystemOutputs() {
        final SettingsCombiner settingsCombiner = settingsCombiner();
        if (hasOutputPort(UseSettings.SETTINGS_NAME_OUTPUT_NAME)) {
            getScalar(UseSettings.SETTINGS_NAME_OUTPUT_NAME).setTo(settingsCombiner.name());
        }
    }
}

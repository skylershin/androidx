/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.watchface.style

import android.graphics.RectF
import android.graphics.drawable.Icon
import android.support.wearable.complications.ComplicationData
import androidx.annotation.RestrictTo
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.watchface.style.data.ComplicationsUserStyleSettingWireFormat
import androidx.wear.watchface.style.data.ComplicationsUserStyleSettingWireFormat.ComplicationOverlayWireFormat
import androidx.wear.watchface.style.data.ComplicationsUserStyleSettingWireFormat.ComplicationsOptionWireFormat
import java.security.InvalidParameterException

/**
 * ComplicationsUserStyleSetting is the recommended [UserStyleSetting] for representing
 * complication configuration options such as the number of active complications, their location,
 * etc... The [ComplicationsOption] class allows you to apply a list of [ComplicationOverlay]s on
 * top of the base config as specified by the [androidx.wear.watchface.Complication] constructor.
 *
 * The ComplicationsManager listens for style changes with this setting and when a
 * [ComplicationsOption] is selected the overrides are automatically applied. Note its suggested
 * that the default [ComplicationOverlay] (the first entry in the list) does not apply any
 * overrides.
 *
 * Not to be confused with complication provider selection.
 */
public class ComplicationsUserStyleSetting : UserStyleSetting {

    /**
     * Overrides to be applied to the corresponding complication's initial config (as specified in
     * [androidx.wear.watchface.Complication]) when the setting is selected.
     */
    public class ComplicationOverlay internal constructor(
        /** The id of the complication to configure. */
        public val complicationId: Int,

        /**
         * If non null, whether the complication should be enabled for this configuration. If null
         * then no changes are made.
         */
        @get:JvmName("isEnabled")
        public val enabled: Boolean?,

        /**
         * If non null, the new unit square screen space complication bounds for this configuration.
         * If null then no changes are made.
         */
        public val bounds: RectF?,

        /**
         * If non null, the new types of complication supported by this complication for this
         * configuration. If null then no changes are made.
         */
        public val supportedTypes: IntArray?,

        /**
         * If non null, the new default complication provider for this configuration. If null then
         * no changes are made.
         */
        public val defaultProviderPolicy: DefaultComplicationProviderPolicy?,

        /**
         * If non null, the new default complication provider data type. See
         * [ComplicationData .ComplicationType].  If null then no changes are made.
         */
        @get:SuppressWarnings("AutoBoxing")
        public val defaultProviderType: Int?
    ) {
        public class Builder(
            /** The id of the complication to configure. */
            private val complicationId: Int
        ) {
            private var enabled: Boolean? = null
            private var bounds: RectF? = null
            private var supportedTypes: IntArray? = null
            private var defaultComplicationProviderPolicy: DefaultComplicationProviderPolicy? = null
            private var defaultComplicationProviderType: Int? = null

            /** Overrides the complication's enabled flag. */
            public fun setEnabled(enabled: Boolean): Builder = apply {
                this.enabled = enabled
            }

            /** Overrides the complication's unit-square screen space bounds. */
            public fun setBounds(bounds: RectF): Builder = apply {
                this.bounds = bounds
            }

            /** Overrides the complication's supported complication types. */
            public fun setSupportedTypes(supportedTypes: IntArray): Builder = apply {
                this.supportedTypes = supportedTypes
            }

            /** Overrides the complication's [DefaultComplicationProviderPolicy]. */
            public fun setDefaultProviderPolicy(
                defaultComplicationProviderPolicy: DefaultComplicationProviderPolicy?
            ): Builder = apply {
                this.defaultComplicationProviderPolicy = defaultComplicationProviderPolicy
            }

            /**
             * Overrides the default complication provider data type. See
             * [ComplicationData.ComplicationType]
             */
            public fun setDefaultProviderType(
                @ComplicationData.ComplicationType defaultComplicationProviderType: Int
            ): Builder = apply {
                this.defaultComplicationProviderType = defaultComplicationProviderType
            }

            public fun build(): ComplicationOverlay = ComplicationOverlay(
                complicationId,
                enabled,
                bounds,
                supportedTypes,
                defaultComplicationProviderPolicy,
                defaultComplicationProviderType
            )
        }

        internal constructor(
            wireFormat: ComplicationOverlayWireFormat
        ) : this(
            wireFormat.mComplicationId,
            when (wireFormat.mEnabled) {
                ComplicationOverlayWireFormat.ENABLED_UNKNOWN -> null
                ComplicationOverlayWireFormat.ENABLED_YES -> true
                ComplicationOverlayWireFormat.ENABLED_NO -> false
                else -> throw InvalidParameterException(
                    "Unrecognised wireFormat.mEnabled " + wireFormat.mEnabled
                )
            },
            wireFormat.mBounds,
            wireFormat.mSupportedTypes,
            wireFormat.mDefaultProviders?.let {
                DefaultComplicationProviderPolicy(it, wireFormat.mSystemProviderFallback)
            },
            if (wireFormat.mDefaultProviderType !=
                ComplicationOverlayWireFormat.NO_DEFAULT_PROVIDER_TYPE
            ) {
                wireFormat.mDefaultProviderType
            } else {
                null
            }
        )

        internal fun toWireFormat() =
            ComplicationOverlayWireFormat(
                complicationId,
                enabled,
                bounds,
                supportedTypes,
                defaultProviderPolicy?.providersAsList(),
                defaultProviderPolicy?.systemProviderFallback,
                defaultProviderType
            )
    }

    public constructor (
        /** Identifier for the element, must be unique. */
        id: String,

        /** Localized human readable name for the element, used in the userStyle selection UI. */
        displayName: CharSequence,

        /** Localized description string displayed under the displayName. */
        description: CharSequence,

        /** Icon for use in the userStyle selection UI. */
        icon: Icon?,

        /**
         * The configuration for affected complications. The first entry is the default value
         */
        complicationConfig: List<ComplicationsOption>,

        /**
         * Used by the style configuration UI. Describes which rendering layers this style affects,
         * must include [Layer.COMPLICATIONS].
         */
        affectsLayers: Collection<Layer>
    ) : super(
        id,
        displayName,
        description,
        icon,
        complicationConfig,
        0,
        affectsLayers
    ) {
        require(affectsLayers.contains(Layer.COMPLICATIONS))
    }

    internal constructor(wireFormat: ComplicationsUserStyleSettingWireFormat) : super(wireFormat)

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun toWireFormat(): ComplicationsUserStyleSettingWireFormat =
        ComplicationsUserStyleSettingWireFormat(
            id,
            displayName,
            description,
            icon,
            getWireFormatOptionsList(),
            defaultOptionIndex,
            affectsLayers.map { it.ordinal }
        )

    /** Represents an override to the initial complication configuration. */
    public class ComplicationsOption : Option {
        /**
         * Overlays to be applied when this ComplicationsOption is selected. If this is empty
         * then the net result is the initial complication configuration.
         */
        public val complicationOverlays: Collection<ComplicationOverlay>

        /** Localized human readable name for the setting, used in the style selection UI. */
        public val displayName: CharSequence

        /** Icon for use in the style selection UI. */
        public val icon: Icon?

        public constructor(
            id: String,
            displayName: CharSequence,
            icon: Icon?,
            complicationOverlays: Collection<ComplicationOverlay>
        ) : super(id) {
            this.complicationOverlays = complicationOverlays
            this.displayName = displayName
            this.icon = icon
        }

        internal constructor(wireFormat: ComplicationsOptionWireFormat) : super(wireFormat.mId) {
            complicationOverlays = wireFormat.mComplicationOverlays.map { ComplicationOverlay(it) }
            displayName = wireFormat.mDisplayName
            icon = wireFormat.mIcon
        }

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat(): ComplicationsOptionWireFormat =
            ComplicationsOptionWireFormat(
                id,
                displayName,
                icon,
                complicationOverlays.map { it.toWireFormat() }.toTypedArray()
            )
    }
}
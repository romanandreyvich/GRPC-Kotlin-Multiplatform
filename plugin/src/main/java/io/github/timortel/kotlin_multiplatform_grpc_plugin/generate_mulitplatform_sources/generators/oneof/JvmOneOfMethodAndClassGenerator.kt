package io.github.timortel.kotlin_multiplatform_grpc_plugin.generate_mulitplatform_sources.generators.oneof

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import io.github.timortel.kotlin_multiplatform_grpc_plugin.generate_mulitplatform_sources.ProtoType
import io.github.timortel.kotlin_multiplatform_grpc_plugin.generate_mulitplatform_sources.content.ProtoMessage
import io.github.timortel.kotlin_multiplatform_grpc_plugin.generate_mulitplatform_sources.content.ProtoOneOf
import io.github.timortel.kotlin_multiplatform_grpc_plugin.generate_mulitplatform_sources.generators.Const

object JvmOneOfMethodAndClassGenerator : OneOfMethodAndClassGenerator(true) {

    override val attrs: List<KModifier> = listOf(KModifier.ACTUAL)

    override fun modifyOneOfProperty(
        builder: PropertySpec.Builder,
        message: ProtoMessage,
        oneOf: ProtoOneOf
    ) {
        val implName = Const.Message.Constructor.JVM.PARAM_IMPL

        val jvmProtoEnumClassName = "${oneOf.capitalizedName}Case"

        builder.getter(
            FunSpec
                .getterBuilder()
                .apply {
                    addCode("return when(%N.get${oneOf.capitalizedName}Case()) {\n", implName)
                    //simply map each enum to the km enum
                    oneOf.attributes.forEach { attr ->
                        addCode(
                            "%T.%N.%N -> %T(",
                            message.jvmType,
                            jvmProtoEnumClassName,
                            attr.originalName.uppercase(),
                            Const.Message.OneOf.childClassName(message, oneOf, attr),
                            )

                        if (attr.types.protoType == ProtoType.ENUM) {
                            addCode("%T.%N(%N.%N.number)",
                                attr.commonType,
                                Const.Enum.getEnumForNumFunctionName,
                                implName,
                                Const.Message.Attribute.Scalar.JVM.getFunction(message, attr).simpleName
                            )
                        } else {
                            if (attr.types.protoType == ProtoType.MESSAGE) {
                                addCode("%M(", Const.Message.CommonFunction.JVM.commonFunction(attr))
                            }

                            addCode(
                                "%N.%N",
                                implName,
                                Const.Message.Attribute.Scalar.JVM.getFunction(message, attr).simpleName
                            )

                            if (attr.types.protoType == ProtoType.MESSAGE) {
                                addCode(")")
                            }
                        }

                        addCode(")\n")
                    }
                    //Add case for when it's not set
                    addCode(
                        "%T.%N.%N -> %T\n",
                        message.jvmType, jvmProtoEnumClassName, "${oneOf.name.uppercase()}_NOT_SET",
                        Const.Message.OneOf.notSetClassName(message, oneOf)
                    )
                    //Unknown case
                    addCode("else -> %T", Const.Message.OneOf.unknownClassName(message, oneOf))
                    addCode("\n}")
                }
                .build()
        )
    }
}
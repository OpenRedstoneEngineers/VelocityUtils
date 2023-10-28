package org.openredstone.velocityutils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

fun Component.resolveTo(that: String) = TagResolver.resolver(that, Tag.inserting(this))

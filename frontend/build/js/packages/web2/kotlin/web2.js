(function (root, factory) {
  if (typeof define === 'function' && define.amd)
    define(['exports', 'kotlin'], factory);
  else if (typeof exports === 'object')
    factory(module.exports, require('kotlin'));
  else {
    if (typeof kotlin === 'undefined') {
      throw new Error("Error loading module 'web2'. Its dependency 'kotlin' was not found. Please, check whether 'kotlin' is loaded prior to 'web2'.");
    }
    root.web2 = factory(typeof web2 === 'undefined' ? {} : web2, kotlin);
  }
}(this, function (_, Kotlin) {
  'use strict';
  var $$importsForInline$$ = _.$$importsForInline$$ || (_.$$importsForInline$$ = {});
  var Unit = Kotlin.kotlin.Unit;
  var HashSet_init = Kotlin.kotlin.collections.HashSet_init_287e2$;
  var Kind_CLASS = Kotlin.Kind.CLASS;
  var emptySet = Kotlin.kotlin.collections.emptySet_287e2$;
  var plus = Kotlin.kotlin.collections.plus_khz7k3$;
  var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
  var setOf = Kotlin.kotlin.collections.setOf_i5x0yv$;
  var Kind_OBJECT = Kotlin.Kind.OBJECT;
  var ensureNotNull = Kotlin.ensureNotNull;
  var toMap = Kotlin.kotlin.collections.toMap_v2dak7$;
  var StringBuilder_init = Kotlin.kotlin.text.StringBuilder_init;
  var startsWith = Kotlin.kotlin.text.startsWith_7epoxm$;
  var replace = Kotlin.kotlin.text.replace_680rmw$;
  var to = Kotlin.kotlin.to_ujzrz7$;
  var ArrayList_init = Kotlin.kotlin.collections.ArrayList_init_287e2$;
  var HashMap_init = Kotlin.kotlin.collections.HashMap_init_q3lmfv$;
  var RuntimeException_init = Kotlin.kotlin.RuntimeException_init_pdl1vj$;
  var RuntimeException = Kotlin.kotlin.RuntimeException;
  var PropertyMetadata = Kotlin.PropertyMetadata;
  var throwCCE = Kotlin.throwCCE;
  var iterator = Kotlin.kotlin.text.iterator_gw00vp$;
  var unboxChar = Kotlin.unboxChar;
  var Kind_INTERFACE = Kotlin.Kind.INTERFACE;
  var toString = Kotlin.toString;
  var plus_0 = Kotlin.kotlin.collections.plus_xfiyik$;
  var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
  var Element_0 = Element;
  var toChar = Kotlin.toChar;
  var emptyList = Kotlin.kotlin.collections.emptyList_287e2$;
  var throwUPAE = Kotlin.throwUPAE;
  var plus_1 = Kotlin.kotlin.collections.plus_mydzjv$;
  var toSet = Kotlin.kotlin.collections.toSet_7wnvza$;
  var toList = Kotlin.kotlin.collections.toList_7wnvza$;
  var zip = Kotlin.kotlin.collections.zip_45mdf7$;
  var addAll = Kotlin.kotlin.collections.addAll_ipc267$;
  var collectionSizeOrDefault = Kotlin.kotlin.collections.collectionSizeOrDefault_ba2ldo$;
  var ArrayList_init_0 = Kotlin.kotlin.collections.ArrayList_init_ww73n8$;
  var removePrefix = Kotlin.kotlin.text.removePrefix_gsj5wt$;
  var toInt = Kotlin.kotlin.text.toInt_6ic1pp$;
  var IllegalArgumentException_init = Kotlin.kotlin.IllegalArgumentException_init_pdl1vj$;
  var Math_0 = Math;
  var defineInlineFunction = Kotlin.defineInlineFunction;
  var wrapFunction = Kotlin.wrapFunction;
  var Enum = Kotlin.kotlin.Enum;
  var throwISE = Kotlin.throwISE;
  var toMutableMap = Kotlin.kotlin.collections.toMutableMap_abgq59$;
  var emptyMap = Kotlin.kotlin.collections.emptyMap_q3lmfv$;
  var toInt_0 = Kotlin.kotlin.text.toInt_pdl1vz$;
  var Result = Kotlin.kotlin.Result;
  var Throwable = Error;
  var createFailure = Kotlin.kotlin.createFailure_tcv7n7$;
  var get_indices = Kotlin.kotlin.collections.get_indices_gzk92b$;
  var clear = Kotlin.kotlin.dom.clear_asww5s$;
  var listOf = Kotlin.kotlin.collections.listOf_i5x0yv$;
  var toBoxedChar = Kotlin.toBoxedChar;
  var String_0 = Kotlin.kotlin.text.String_4hbowm$;
  var charArray = Kotlin.charArray;
  var IntRange = Kotlin.kotlin.ranges.IntRange;
  var split = Kotlin.kotlin.text.split_ip8yn$;
  var getOrNull = Kotlin.kotlin.collections.getOrNull_yzln2o$;
  var lastOrNull = Kotlin.kotlin.collections.lastOrNull_2p1efm$;
  var equals = Kotlin.equals;
  var firstOrNull = Kotlin.kotlin.collections.firstOrNull_2p1efm$;
  var checkIndexOverflow = Kotlin.kotlin.collections.checkIndexOverflow_za3lpa$;
  var setOf_0 = Kotlin.kotlin.collections.setOf_mh5how$;
  var mapOf = Kotlin.kotlin.collections.mapOf_x2b85n$;
  var L1000 = Kotlin.Long.fromInt(1000);
  var Any = Object;
  CSSBuilder.prototype = Object.create(CSSPropertyListBuilder.prototype);
  CSSBuilder.prototype.constructor = CSSBuilder;
  WriteOnlyProperty.prototype = Object.create(RuntimeException.prototype);
  WriteOnlyProperty.prototype.constructor = WriteOnlyProperty;
  WrapType.prototype = Object.create(Enum.prototype);
  WrapType.prototype.constructor = WrapType;
  RouteSegment$Plain.prototype = Object.create(RouteSegment.prototype);
  RouteSegment$Plain.prototype.constructor = RouteSegment$Plain;
  RouteSegment$Remaining.prototype = Object.create(RouteSegment.prototype);
  RouteSegment$Remaining.prototype.constructor = RouteSegment$Remaining;
  RouteSegment$Wildcard.prototype = Object.create(RouteSegment.prototype);
  RouteSegment$Wildcard.prototype.constructor = RouteSegment$Wildcard;
  ToastType.prototype = Object.create(Enum.prototype);
  ToastType.prototype.constructor = ToastType;
  CoursesBackend.prototype = Object.create(RPCNamespace.prototype);
  CoursesBackend.prototype.constructor = CoursesBackend;
  Page.prototype = Object.create(Enum.prototype);
  Page.prototype.constructor = Page;
  function BoundData(initialValue) {
    this.handlers_0 = HashSet_init();
    this.currentValue_h4sgho$_0 = initialValue;
  }
  Object.defineProperty(BoundData.prototype, 'currentValue', {
    get: function () {
      return this.currentValue_h4sgho$_0;
    },
    set: function (value) {
      this.currentValue_h4sgho$_0 = value;
      var tmp$;
      tmp$ = this.handlers_0.iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        element(value);
      }
    }
  });
  BoundData.prototype.addHandler_qlkmfe$ = function (handler) {
    this.handlers_0.add_11rb$(handler);
    handler(this.currentValue);
    return handler;
  };
  BoundData.prototype.removeHandler_qlkmfe$ = function (handler) {
    this.handlers_0.remove_11rb$(handler);
  };
  BoundData.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'BoundData',
    interfaces: []
  };
  function boundText$lambda(closure$template, closure$node) {
    return function (it) {
      closure$node.nodeValue = closure$template(it);
      return Unit;
    };
  }
  function boundText($receiver, data, template) {
    var node = text($receiver, '');
    data.addHandler_qlkmfe$(boundText$lambda(template, node));
  }
  function boundClass$lambda(closure$classes, closure$baseClasses, closure$existingClasses, closure$node) {
    return function (newData) {
      closure$node.className = joinToString(plus(closure$classes(newData), closure$baseClasses), ' ') + ' ' + closure$existingClasses;
      return Unit;
    };
  }
  function boundClass($receiver, data, baseClasses, classes) {
    if (baseClasses === void 0)
      baseClasses = emptySet();
    var node = $receiver;
    var existingClasses = node.className;
    data.addHandler_qlkmfe$(boundClass$lambda(classes, baseClasses, existingClasses, node));
  }
  function boundClassByPredicate$lambda(closure$predicate, closure$classes, closure$baseClasses, closure$node) {
    return function (newData) {
      if (closure$predicate(newData)) {
        closure$node.className = joinToString(plus(setOf(closure$classes.slice()), closure$baseClasses), ' ');
      }
       else {
        closure$node.className = joinToString(closure$baseClasses, ' ');
      }
      return Unit;
    };
  }
  function boundClassByPredicate($receiver, data, classes, baseClasses, predicate) {
    if (baseClasses === void 0)
      baseClasses = emptySet();
    var node = $receiver;
    data.addHandler_qlkmfe$(boundClassByPredicate$lambda(predicate, classes, baseClasses, node));
  }
  function baseElement$lambda($receiver) {
    return Unit;
  }
  function CSS() {
    CSS_instance = this;
    this.styleReference = new Reference();
  }
  CSS.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'CSS',
    interfaces: []
  };
  var CSS_instance = null;
  function CSS_getInstance() {
    if (CSS_instance === null) {
      new CSS();
    }
    return CSS_instance;
  }
  function rawCSS(style) {
    if (CSS_getInstance().styleReference.currentOrNull == null) {
      var $receiver = ensureNotNull(document.body);
      var attrs = new CommonAttributes(void 0, void 0, void 0, CSS_getInstance().styleReference);
      var tmp$;
      var element = document.createElement('style');
      var tmp$_0;
      tmp$_0 = attrs.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs.classes.isEmpty() || attrs.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus_0(attrs.classes, attrs.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs.ref != null)
        attrs.ref.currentOrNull = t;
      baseElement$lambda(t);
      $receiver.appendChild(element);
    }
    ensureNotNull(CSS_getInstance().styleReference.currentOrNull).innerHTML = ensureNotNull(CSS_getInstance().styleReference.currentOrNull).innerHTML + style;
  }
  function globalCSS(selector, rules) {
    globalCSS_0(selector, toMap(rules));
  }
  function globalCSS_0(selector, rules) {
    var cssRuleBuilder = StringBuilder_init();
    cssRuleBuilder.append_gw00v9$(selector + ' {' + '\n');
    var tmp$;
    tmp$ = rules.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var k = element.key;
      var v = element.value;
      cssRuleBuilder.append_gw00v9$('  ' + k + ':' + v + ';' + '\n');
    }
    cssRuleBuilder.append_gw00v9$('}\n');
    rawCSS(cssRuleBuilder.toString());
  }
  var cssNamespaceId;
  function css(builder) {
    var tmp$;
    var className = 'c-' + (tmp$ = cssNamespaceId, cssNamespaceId = tmp$ + 1 | 0, tmp$);
    var $receiver = new CSSBuilder();
    builder($receiver);
    var cssBuilder = $receiver;
    globalCSS_0('.' + className, cssBuilder.properties);
    var tmp$_0;
    tmp$_0 = cssBuilder.rules.iterator();
    while (tmp$_0.hasNext()) {
      var element = tmp$_0.next();
      var selector = element.component1()
      , props = element.component2();
      if (startsWith(selector, SELF_SELECTOR)) {
        var resolvedSelector = replace(selector, SELF_SELECTOR, className);
        globalCSS_0('.' + resolvedSelector, props);
      }
       else {
        globalCSS_0('.' + className + ' ' + selector, props);
      }
    }
    return className;
  }
  function CSSBuilder() {
    CSSPropertyListBuilder.call(this);
    this.rules = ArrayList_init();
  }
  CSSBuilder.prototype.invoke_pt2paz$ = function ($receiver, builder) {
    var tmp$ = this.rules;
    var tmp$_0 = $receiver.textValue;
    var $receiver_0 = new CSSPropertyListBuilder();
    builder($receiver_0);
    tmp$.add_11rb$(to(tmp$_0, $receiver_0.properties));
  };
  CSSBuilder.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'CSSBuilder',
    interfaces: [CSSPropertyListBuilder, CSSSelectorContext]
  };
  function CSSPropertyListBuilder() {
    this.properties = HashMap_init();
  }
  CSSPropertyListBuilder.prototype.add_puj7f4$ = function (property, value) {
    this.properties.put_xwzc9p$(property, value);
  };
  CSSPropertyListBuilder.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'CSSPropertyListBuilder',
    interfaces: []
  };
  function WriteOnlyProperty() {
    RuntimeException_init('Write only property', this);
    this.name = 'WriteOnlyProperty';
  }
  WriteOnlyProperty.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'WriteOnlyProperty',
    interfaces: [RuntimeException]
  };
  var textDecoration;
  var textDecoration_metadata = new PropertyMetadata('textDecoration');
  function get_textDecoration($receiver) {
    return textDecoration.getValue_n5byny$($receiver, textDecoration_metadata);
  }
  function set_textDecoration($receiver, textDecoration_0) {
    textDecoration.setValue_inbhhw$($receiver, textDecoration_metadata, textDecoration_0);
  }
  var color;
  var color_metadata = new PropertyMetadata('color');
  function get_color($receiver) {
    return color.getValue_n5byny$($receiver, color_metadata);
  }
  function set_color($receiver, color_0) {
    color.setValue_inbhhw$($receiver, color_metadata, color_0);
  }
  var transition;
  var transition_metadata = new PropertyMetadata('transition');
  function get_transition($receiver) {
    return transition.getValue_n5byny$($receiver, transition_metadata);
  }
  function set_transition($receiver, transition_0) {
    transition.setValue_inbhhw$($receiver, transition_metadata, transition_0);
  }
  var position;
  var position_metadata = new PropertyMetadata('position');
  function get_position($receiver) {
    return position.getValue_n5byny$($receiver, position_metadata);
  }
  function set_position($receiver, position_0) {
    position.setValue_inbhhw$($receiver, position_metadata, position_0);
  }
  var top;
  var top_metadata = new PropertyMetadata('top');
  function get_top($receiver) {
    return top.getValue_n5byny$($receiver, top_metadata);
  }
  function set_top($receiver, top_0) {
    top.setValue_inbhhw$($receiver, top_metadata, top_0);
  }
  var bottom;
  var bottom_metadata = new PropertyMetadata('bottom');
  function get_bottom($receiver) {
    return bottom.getValue_n5byny$($receiver, bottom_metadata);
  }
  function set_bottom($receiver, bottom_0) {
    bottom.setValue_inbhhw$($receiver, bottom_metadata, bottom_0);
  }
  var left;
  var left_metadata = new PropertyMetadata('left');
  function get_left($receiver) {
    return left.getValue_n5byny$($receiver, left_metadata);
  }
  function set_left($receiver, left_0) {
    left.setValue_inbhhw$($receiver, left_metadata, left_0);
  }
  var right;
  var right_metadata = new PropertyMetadata('right');
  function get_right($receiver) {
    return right.getValue_n5byny$($receiver, right_metadata);
  }
  function set_right($receiver, right_0) {
    right.setValue_inbhhw$($receiver, right_metadata, right_0);
  }
  var backgroundColor;
  var backgroundColor_metadata = new PropertyMetadata('backgroundColor');
  function get_backgroundColor($receiver) {
    return backgroundColor.getValue_n5byny$($receiver, backgroundColor_metadata);
  }
  function set_backgroundColor($receiver, backgroundColor_0) {
    backgroundColor.setValue_inbhhw$($receiver, backgroundColor_metadata, backgroundColor_0);
  }
  var content;
  var content_metadata = new PropertyMetadata('content');
  function get_content($receiver) {
    return content.getValue_n5byny$($receiver, content_metadata);
  }
  function set_content($receiver, content_0) {
    content.setValue_inbhhw$($receiver, content_metadata, content_0);
  }
  var opacity;
  var opacity_metadata = new PropertyMetadata('opacity');
  function get_opacity($receiver) {
    return opacity.getValue_n5byny$($receiver, opacity_metadata);
  }
  function set_opacity($receiver, opacity_0) {
    opacity.setValue_inbhhw$($receiver, opacity_metadata, opacity_0);
  }
  var outline;
  var outline_metadata = new PropertyMetadata('outline');
  function get_outline($receiver) {
    return outline.getValue_n5byny$($receiver, outline_metadata);
  }
  function set_outline($receiver, outline_0) {
    outline.setValue_inbhhw$($receiver, outline_metadata, outline_0);
  }
  var display;
  var display_metadata = new PropertyMetadata('display');
  function get_display($receiver) {
    return display.getValue_n5byny$($receiver, display_metadata);
  }
  function set_display($receiver, display_0) {
    display.setValue_inbhhw$($receiver, display_metadata, display_0);
  }
  var padding;
  var padding_metadata = new PropertyMetadata('padding');
  function get_padding($receiver) {
    return padding.getValue_n5byny$($receiver, padding_metadata);
  }
  function set_padding($receiver, padding_0) {
    padding.setValue_inbhhw$($receiver, padding_metadata, padding_0);
  }
  var paddingTop;
  var paddingTop_metadata = new PropertyMetadata('paddingTop');
  function get_paddingTop($receiver) {
    return paddingTop.getValue_n5byny$($receiver, paddingTop_metadata);
  }
  function set_paddingTop($receiver, paddingTop_0) {
    paddingTop.setValue_inbhhw$($receiver, paddingTop_metadata, paddingTop_0);
  }
  var paddingBottom;
  var paddingBottom_metadata = new PropertyMetadata('paddingBottom');
  function get_paddingBottom($receiver) {
    return paddingBottom.getValue_n5byny$($receiver, paddingBottom_metadata);
  }
  function set_paddingBottom($receiver, paddingBottom_0) {
    paddingBottom.setValue_inbhhw$($receiver, paddingBottom_metadata, paddingBottom_0);
  }
  var paddingLeft;
  var paddingLeft_metadata = new PropertyMetadata('paddingLeft');
  function get_paddingLeft($receiver) {
    return paddingLeft.getValue_n5byny$($receiver, paddingLeft_metadata);
  }
  function set_paddingLeft($receiver, paddingLeft_0) {
    paddingLeft.setValue_inbhhw$($receiver, paddingLeft_metadata, paddingLeft_0);
  }
  var paddingRight;
  var paddingRight_metadata = new PropertyMetadata('paddingRight');
  function get_paddingRight($receiver) {
    return paddingRight.getValue_n5byny$($receiver, paddingRight_metadata);
  }
  function set_paddingRight($receiver, paddingRight_0) {
    paddingRight.setValue_inbhhw$($receiver, paddingRight_metadata, paddingRight_0);
  }
  var border;
  var border_metadata = new PropertyMetadata('border');
  function get_border($receiver) {
    return border.getValue_n5byny$($receiver, border_metadata);
  }
  function set_border($receiver, border_0) {
    border.setValue_inbhhw$($receiver, border_metadata, border_0);
  }
  var borderRadius;
  var borderRadius_metadata = new PropertyMetadata('borderRadius');
  function get_borderRadius($receiver) {
    return borderRadius.getValue_n5byny$($receiver, borderRadius_metadata);
  }
  function set_borderRadius($receiver, borderRadius_0) {
    borderRadius.setValue_inbhhw$($receiver, borderRadius_metadata, borderRadius_0);
  }
  var width;
  var width_metadata = new PropertyMetadata('width');
  function get_width($receiver) {
    return width.getValue_n5byny$($receiver, width_metadata);
  }
  function set_width($receiver, width_0) {
    width.setValue_inbhhw$($receiver, width_metadata, width_0);
  }
  var height;
  var height_metadata = new PropertyMetadata('height');
  function get_height($receiver) {
    return height.getValue_n5byny$($receiver, height_metadata);
  }
  function set_height($receiver, height_0) {
    height.setValue_inbhhw$($receiver, height_metadata, height_0);
  }
  var margin;
  var margin_metadata = new PropertyMetadata('margin');
  function get_margin($receiver) {
    return margin.getValue_n5byny$($receiver, margin_metadata);
  }
  function set_margin($receiver, margin_0) {
    margin.setValue_inbhhw$($receiver, margin_metadata, margin_0);
  }
  var marginTop;
  var marginTop_metadata = new PropertyMetadata('marginTop');
  function get_marginTop($receiver) {
    return marginTop.getValue_n5byny$($receiver, marginTop_metadata);
  }
  function set_marginTop($receiver, marginTop_0) {
    marginTop.setValue_inbhhw$($receiver, marginTop_metadata, marginTop_0);
  }
  var marginLeft;
  var marginLeft_metadata = new PropertyMetadata('marginLeft');
  function get_marginLeft($receiver) {
    return marginLeft.getValue_n5byny$($receiver, marginLeft_metadata);
  }
  function set_marginLeft($receiver, marginLeft_0) {
    marginLeft.setValue_inbhhw$($receiver, marginLeft_metadata, marginLeft_0);
  }
  var marginRight;
  var marginRight_metadata = new PropertyMetadata('marginRight');
  function get_marginRight($receiver) {
    return marginRight.getValue_n5byny$($receiver, marginRight_metadata);
  }
  function set_marginRight($receiver, marginRight_0) {
    marginRight.setValue_inbhhw$($receiver, marginRight_metadata, marginRight_0);
  }
  var marginBottom;
  var marginBottom_metadata = new PropertyMetadata('marginBottom');
  function get_marginBottom($receiver) {
    return marginBottom.getValue_n5byny$($receiver, marginBottom_metadata);
  }
  function set_marginBottom($receiver, marginBottom_0) {
    marginBottom.setValue_inbhhw$($receiver, marginBottom_metadata, marginBottom_0);
  }
  var alignItems;
  var alignItems_metadata = new PropertyMetadata('alignItems');
  function get_alignItems($receiver) {
    return alignItems.getValue_n5byny$($receiver, alignItems_metadata);
  }
  function set_alignItems($receiver, alignItems_0) {
    alignItems.setValue_inbhhw$($receiver, alignItems_metadata, alignItems_0);
  }
  var justifyContent;
  var justifyContent_metadata = new PropertyMetadata('justifyContent');
  function get_justifyContent($receiver) {
    return justifyContent.getValue_n5byny$($receiver, justifyContent_metadata);
  }
  function set_justifyContent($receiver, justifyContent_0) {
    justifyContent.setValue_inbhhw$($receiver, justifyContent_metadata, justifyContent_0);
  }
  var justifyItems;
  var justifyItems_metadata = new PropertyMetadata('justifyItems');
  function get_justifyItems($receiver) {
    return justifyItems.getValue_n5byny$($receiver, justifyItems_metadata);
  }
  function set_justifyItems($receiver, justifyItems_0) {
    justifyItems.setValue_inbhhw$($receiver, justifyItems_metadata, justifyItems_0);
  }
  var flexDirection;
  var flexDirection_metadata = new PropertyMetadata('flexDirection');
  function get_flexDirection($receiver) {
    return flexDirection.getValue_n5byny$($receiver, flexDirection_metadata);
  }
  function set_flexDirection($receiver, flexDirection_0) {
    flexDirection.setValue_inbhhw$($receiver, flexDirection_metadata, flexDirection_0);
  }
  var flexFlow;
  var flexFlow_metadata = new PropertyMetadata('flexFlow');
  function get_flexFlow($receiver) {
    return flexFlow.getValue_n5byny$($receiver, flexFlow_metadata);
  }
  function set_flexFlow($receiver, flexFlow_0) {
    flexFlow.setValue_inbhhw$($receiver, flexFlow_metadata, flexFlow_0);
  }
  var flexGrow;
  var flexGrow_metadata = new PropertyMetadata('flexGrow');
  function get_flexGrow($receiver) {
    return flexGrow.getValue_n5byny$($receiver, flexGrow_metadata);
  }
  function set_flexGrow($receiver, flexGrow_0) {
    flexGrow.setValue_inbhhw$($receiver, flexGrow_metadata, flexGrow_0);
  }
  var flexShrink;
  var flexShrink_metadata = new PropertyMetadata('flexShrink');
  function get_flexShrink($receiver) {
    return flexShrink.getValue_n5byny$($receiver, flexShrink_metadata);
  }
  function set_flexShrink($receiver, flexShrink_0) {
    flexShrink.setValue_inbhhw$($receiver, flexShrink_metadata, flexShrink_0);
  }
  var flexBasis;
  var flexBasis_metadata = new PropertyMetadata('flexBasis');
  function get_flexBasis($receiver) {
    return flexBasis.getValue_n5byny$($receiver, flexBasis_metadata);
  }
  function set_flexBasis($receiver, flexBasis_0) {
    flexBasis.setValue_inbhhw$($receiver, flexBasis_metadata, flexBasis_0);
  }
  var boxSizing;
  var boxSizing_metadata = new PropertyMetadata('boxSizing');
  function get_boxSizing($receiver) {
    return boxSizing.getValue_n5byny$($receiver, boxSizing_metadata);
  }
  function set_boxSizing($receiver, boxSizing_0) {
    boxSizing.setValue_inbhhw$($receiver, boxSizing_metadata, boxSizing_0);
  }
  var resize;
  var resize_metadata = new PropertyMetadata('resize');
  function get_resize($receiver) {
    return resize.getValue_n5byny$($receiver, resize_metadata);
  }
  function set_resize($receiver, resize_0) {
    resize.setValue_inbhhw$($receiver, resize_metadata, resize_0);
  }
  var fontSize;
  var fontSize_metadata = new PropertyMetadata('fontSize');
  function get_fontSize($receiver) {
    return fontSize.getValue_n5byny$($receiver, fontSize_metadata);
  }
  function set_fontSize($receiver, fontSize_0) {
    fontSize.setValue_inbhhw$($receiver, fontSize_metadata, fontSize_0);
  }
  var fontWeight;
  var fontWeight_metadata = new PropertyMetadata('fontWeight');
  function get_fontWeight($receiver) {
    return fontWeight.getValue_n5byny$($receiver, fontWeight_metadata);
  }
  function set_fontWeight($receiver, fontWeight_0) {
    fontWeight.setValue_inbhhw$($receiver, fontWeight_metadata, fontWeight_0);
  }
  var fontFamily;
  var fontFamily_metadata = new PropertyMetadata('fontFamily');
  function get_fontFamily($receiver) {
    return fontFamily.getValue_n5byny$($receiver, fontFamily_metadata);
  }
  function set_fontFamily($receiver, fontFamily_0) {
    fontFamily.setValue_inbhhw$($receiver, fontFamily_metadata, fontFamily_0);
  }
  var listStyle;
  var listStyle_metadata = new PropertyMetadata('listStyle');
  function get_listStyle($receiver) {
    return listStyle.getValue_n5byny$($receiver, listStyle_metadata);
  }
  function set_listStyle($receiver, listStyle_0) {
    listStyle.setValue_inbhhw$($receiver, listStyle_metadata, listStyle_0);
  }
  var maxWidth;
  var maxWidth_metadata = new PropertyMetadata('maxWidth');
  function get_maxWidth($receiver) {
    return maxWidth.getValue_n5byny$($receiver, maxWidth_metadata);
  }
  function set_maxWidth($receiver, maxWidth_0) {
    maxWidth.setValue_inbhhw$($receiver, maxWidth_metadata, maxWidth_0);
  }
  var maxHeight;
  var maxHeight_metadata = new PropertyMetadata('maxHeight');
  function get_maxHeight($receiver) {
    return maxHeight.getValue_n5byny$($receiver, maxHeight_metadata);
  }
  function set_maxHeight($receiver, maxHeight_0) {
    maxHeight.setValue_inbhhw$($receiver, maxHeight_metadata, maxHeight_0);
  }
  var minHeight;
  var minHeight_metadata = new PropertyMetadata('minHeight');
  function get_minHeight($receiver) {
    return minHeight.getValue_n5byny$($receiver, minHeight_metadata);
  }
  function set_minHeight($receiver, minHeight_0) {
    minHeight.setValue_inbhhw$($receiver, minHeight_metadata, minHeight_0);
  }
  var minWidth;
  var minWidth_metadata = new PropertyMetadata('minWidth');
  function get_minWidth($receiver) {
    return minWidth.getValue_n5byny$($receiver, minWidth_metadata);
  }
  function set_minWidth($receiver, minWidth_0) {
    minWidth.setValue_inbhhw$($receiver, minWidth_metadata, minWidth_0);
  }
  var borderCollapse;
  var borderCollapse_metadata = new PropertyMetadata('borderCollapse');
  function get_borderCollapse($receiver) {
    return borderCollapse.getValue_n5byny$($receiver, borderCollapse_metadata);
  }
  function set_borderCollapse($receiver, borderCollapse_0) {
    borderCollapse.setValue_inbhhw$($receiver, borderCollapse_metadata, borderCollapse_0);
  }
  var borderSpacing;
  var borderSpacing_metadata = new PropertyMetadata('borderSpacing');
  function get_borderSpacing($receiver) {
    return borderSpacing.getValue_n5byny$($receiver, borderSpacing_metadata);
  }
  function set_borderSpacing($receiver, borderSpacing_0) {
    borderSpacing.setValue_inbhhw$($receiver, borderSpacing_metadata, borderSpacing_0);
  }
  var textAlign;
  var textAlign_metadata = new PropertyMetadata('textAlign');
  function get_textAlign($receiver) {
    return textAlign.getValue_n5byny$($receiver, textAlign_metadata);
  }
  function set_textAlign($receiver, textAlign_0) {
    textAlign.setValue_inbhhw$($receiver, textAlign_metadata, textAlign_0);
  }
  var boxShadow;
  var boxShadow_metadata = new PropertyMetadata('boxShadow');
  function get_boxShadow($receiver) {
    return boxShadow.getValue_n5byny$($receiver, boxShadow_metadata);
  }
  function set_boxShadow($receiver, boxShadow_0) {
    boxShadow.setValue_inbhhw$($receiver, boxShadow_metadata, boxShadow_0);
  }
  function CSSDelegate(name) {
    if (name === void 0)
      name = null;
    this.name = name;
  }
  CSSDelegate.prototype.getValue_n5byny$ = function (thisRef, property) {
    throw new WriteOnlyProperty();
  };
  CSSDelegate.prototype.setValue_inbhhw$ = function (thisRef, property, value) {
    var tmp$, tmp$_0;
    (Kotlin.isType(tmp$ = thisRef, CSSPropertyListBuilder) ? tmp$ : throwCCE()).add_puj7f4$((tmp$_0 = this.name) != null ? tmp$_0 : this.transformName_0(property.callableName), value);
  };
  CSSDelegate.prototype.transformName_0 = function (name) {
    var tmp$;
    var builder = StringBuilder_init();
    tmp$ = iterator(name);
    while (tmp$.hasNext()) {
      var c = unboxChar(tmp$.next());
      if (isUpperCase(c)) {
        builder.append_gw00v9$('-');
        builder.append_s8itvh$(toChar(String.fromCharCode(c | 0).toLowerCase().charCodeAt(0)));
      }
       else {
        builder.append_s8itvh$(c);
      }
    }
    return builder.toString();
  };
  CSSDelegate.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'CSSDelegate',
    interfaces: []
  };
  function isUpperCase($receiver) {
    return toChar(String.fromCharCode($receiver | 0).toUpperCase().charCodeAt(0)) === $receiver;
  }
  function isLowerCase($receiver) {
    return toChar(String.fromCharCode($receiver | 0).toLowerCase().charCodeAt(0)) === $receiver;
  }
  function CSSSelector(textValue) {
    this.textValue = textValue;
  }
  CSSSelector.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'CSSSelector',
    interfaces: []
  };
  CSSSelector.prototype.component1 = function () {
    return this.textValue;
  };
  CSSSelector.prototype.copy_61zpoe$ = function (textValue) {
    return new CSSSelector(textValue === void 0 ? this.textValue : textValue);
  };
  CSSSelector.prototype.toString = function () {
    return 'CSSSelector(textValue=' + Kotlin.toString(this.textValue) + ')';
  };
  CSSSelector.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.textValue) | 0;
    return result;
  };
  CSSSelector.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.textValue, other.textValue))));
  };
  function CSSSelectorContext() {
  }
  CSSSelectorContext.$metadata$ = {
    kind: Kind_INTERFACE,
    simpleName: 'CSSSelectorContext',
    interfaces: []
  };
  function byTag($receiver, tagName) {
    return new CSSSelector(tagName);
  }
  function byClass($receiver, className) {
    return new CSSSelector('.' + className);
  }
  function byId($receiver, idName) {
    return new CSSSelector('#' + idName);
  }
  function byNamespace($receiver, namespace) {
    return new CSSSelector(namespace + ':|*');
  }
  function matchAny($receiver) {
    return new CSSSelector('*');
  }
  var SELF_SELECTOR;
  function matchSelf($receiver) {
    return new CSSSelector(SELF_SELECTOR);
  }
  function withNoNamespace($receiver) {
    return new CSSSelector('|*');
  }
  function attributePresent($receiver, tagName, attributeName) {
    return new CSSSelector(tagName + '[' + attributeName + ']');
  }
  function attributeEquals($receiver, tagName, attributeName, value, caseInsensitive) {
    if (caseInsensitive === void 0)
      caseInsensitive = false;
    return new CSSSelector(tagName + '[' + attributeName + '=' + value + (caseInsensitive ? ' i' : '') + ']');
  }
  function attributeListContains($receiver, tagName, attributeName, value, caseInsensitive) {
    if (caseInsensitive === void 0)
      caseInsensitive = false;
    return new CSSSelector(tagName + '[' + attributeName + '~=' + value + (caseInsensitive ? ' i' : '') + ']');
  }
  function attributeEqualsHyphen($receiver, tagName, attributeName, value, caseInsensitive) {
    if (caseInsensitive === void 0)
      caseInsensitive = false;
    return new CSSSelector(tagName + '[' + attributeName + '|=' + value + (caseInsensitive ? ' i' : '') + ']');
  }
  function attributeStartsWith($receiver, tagName, attributeName, value, caseInsensitive) {
    if (caseInsensitive === void 0)
      caseInsensitive = false;
    return new CSSSelector(tagName + '[' + attributeName + '^=' + value + (caseInsensitive ? ' i' : '') + ']');
  }
  function attributeEndsWith($receiver, tagName, attributeName, value, caseInsensitive) {
    if (caseInsensitive === void 0)
      caseInsensitive = false;
    return new CSSSelector(tagName + '[' + attributeName + '$' + '=' + value + (caseInsensitive ? ' i' : '') + ']');
  }
  function attributeContains($receiver, tagName, attributeName, value, caseInsensitive) {
    if (caseInsensitive === void 0)
      caseInsensitive = false;
    return new CSSSelector(tagName + '[' + attributeName + '*=' + value + (caseInsensitive ? ' i' : '') + ']');
  }
  function withPseudoClass($receiver, className) {
    return new CSSSelector($receiver.textValue + ':' + className);
  }
  function withPseudoElement($receiver, element) {
    return new CSSSelector($receiver.textValue + '::' + element);
  }
  function adjacentSibling($receiver, other) {
    return new CSSSelector($receiver.textValue + ' + ' + other.textValue);
  }
  function anySibling($receiver, other) {
    return new CSSSelector($receiver.textValue + ' ~ ' + other.textValue);
  }
  function directChild($receiver, other) {
    return new CSSSelector($receiver.textValue + ' > ' + other.textValue);
  }
  function descendant($receiver, other) {
    return new CSSSelector($receiver.textValue + ' ' + other.textValue);
  }
  function or($receiver, other) {
    return new CSSSelector($receiver.textValue + ', ' + other.textValue);
  }
  function and($receiver, other) {
    return new CSSSelector($receiver.textValue + other.textValue);
  }
  function get_pt($receiver) {
    return $receiver.toString() + 'pt';
  }
  function get_px($receiver) {
    return $receiver.toString() + 'px';
  }
  function get_vh($receiver) {
    return $receiver.toString() + 'vh';
  }
  function get_em($receiver) {
    return $receiver.toString() + 'px';
  }
  function get_percent($receiver) {
    return $receiver.toString() + '%';
  }
  function CSSVar(name) {
    this.name = name;
  }
  CSSVar.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'CSSVar',
    interfaces: []
  };
  CSSVar.prototype.component1 = function () {
    return this.name;
  };
  CSSVar.prototype.copy_61zpoe$ = function (name) {
    return new CSSVar(name === void 0 ? this.name : name);
  };
  CSSVar.prototype.toString = function () {
    return 'CSSVar(name=' + Kotlin.toString(this.name) + ')';
  };
  CSSVar.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.name) | 0;
    return result;
  };
  CSSVar.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.name, other.name))));
  };
  function variable($receiver, v, default_0) {
    if (default_0 === void 0)
      default_0 = null;
    if (default_0 != null) {
      return 'var(--' + v.name + ', ' + toString(default_0) + ')';
    }
     else {
      return 'var(--' + v.name + ')';
    }
  }
  function setVariable($receiver, v, value) {
    $receiver.add_puj7f4$('--' + v.name, value);
  }
  function setVariable_0($receiver, v, value) {
    setVariable($receiver, v, value.toString());
  }
  function boxShadow_0(offsetX, offsetY, blurRadius, spreadRadius, color) {
    if (blurRadius === void 0)
      blurRadius = 0;
    if (spreadRadius === void 0)
      spreadRadius = 0;
    if (color === void 0)
      color = null;
    var $receiver = StringBuilder_init();
    $receiver.append_gw00v9$(offsetX.toString() + 'px ' + offsetY + 'px ');
    $receiver.append_gw00v9$(blurRadius.toString() + 'px ' + spreadRadius + 'px');
    if (color != null) {
      $receiver.append_gw00v9$(' ');
      $receiver.append_gw00v9$(color);
    }
    return $receiver.toString();
  }
  function div$lambda($receiver) {
    return Unit;
  }
  function CardInStack(dependencies, predicate, card) {
    if (dependencies === void 0)
      dependencies = emptyList();
    this.dependencies = dependencies;
    this.predicate = predicate;
    this.card = card;
  }
  CardInStack.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'CardInStack',
    interfaces: []
  };
  CardInStack.prototype.component1 = function () {
    return this.dependencies;
  };
  CardInStack.prototype.component2 = function () {
    return this.predicate;
  };
  CardInStack.prototype.component3 = function () {
    return this.card;
  };
  CardInStack.prototype.copy_5y7ni$ = function (dependencies, predicate, card) {
    return new CardInStack(dependencies === void 0 ? this.dependencies : dependencies, predicate === void 0 ? this.predicate : predicate, card === void 0 ? this.card : card);
  };
  CardInStack.prototype.toString = function () {
    return 'CardInStack(dependencies=' + Kotlin.toString(this.dependencies) + (', predicate=' + Kotlin.toString(this.predicate)) + (', card=' + Kotlin.toString(this.card)) + ')';
  };
  CardInStack.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.dependencies) | 0;
    result = result * 31 + Kotlin.hashCode(this.predicate) | 0;
    result = result * 31 + Kotlin.hashCode(this.card) | 0;
    return result;
  };
  CardInStack.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.dependencies, other.dependencies) && Kotlin.equals(this.predicate, other.predicate) && Kotlin.equals(this.card, other.card)))));
  };
  function cardStack$createRoot(this$cardStack, closure$root) {
    return function () {
      var ref = new Reference();
      var $receiver = this$cardStack;
      var attrs = new CommonAttributes(void 0, void 0, void 0, ref);
      var tmp$;
      var element = document.createElement('div');
      var tmp$_0;
      tmp$_0 = attrs.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs.classes.isEmpty() || attrs.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus_0(attrs.classes, attrs.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs.ref != null)
        attrs.ref.currentOrNull = t;
      div$lambda(t);
      $receiver.appendChild(element);
      closure$root.v = ref.current;
      return closure$root.v == null ? throwUPAE('root') : closure$root.v;
    };
  }
  function cardStack$selectCard(closure$ready, closure$root, closure$createRoot, closure$cards) {
    return function () {
      if (!closure$ready.v)
        return;
      deleteNode(closure$root.v == null ? throwUPAE('root') : closure$root.v);
      closure$createRoot();
      var $receiver = closure$cards;
      var firstOrNull$result;
      firstOrNull$break: do {
        var tmp$;
        for (tmp$ = 0; tmp$ !== $receiver.length; ++tmp$) {
          var element = $receiver[tmp$];
          if (element.predicate()) {
            firstOrNull$result = element;
            break firstOrNull$break;
          }
        }
        firstOrNull$result = null;
      }
       while (false);
      var selectedCard = firstOrNull$result;
      if (selectedCard != null) {
        var closure$root_0 = closure$root;
        selectedCard.card(closure$root_0.v == null ? throwUPAE('root') : closure$root_0.v);
      }
    };
  }
  function cardStack$lambda$lambda(closure$selectCard) {
    return function (it) {
      closure$selectCard();
      return Unit;
    };
  }
  function cardStack$lambda(closure$allDependencies, closure$handlers) {
    return function () {
      var tmp$;
      tmp$ = zip(closure$allDependencies, closure$handlers).iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        var dep = element.component1()
        , handler = element.component2();
        var tmp$_0;
        dep.removeHandler_qlkmfe$(typeof (tmp$_0 = handler) === 'function' ? tmp$_0 : throwCCE());
      }
      return Unit;
    };
  }
  function cardStack($receiver, cards, dependencies) {
    if (dependencies === void 0)
      dependencies = emptyList();
    var ready = {v: false};
    var root = {v: null};
    var createRoot = cardStack$createRoot($receiver, root);
    var selectCard = cardStack$selectCard(ready, root, createRoot, cards);
    var destination = ArrayList_init();
    var tmp$;
    for (tmp$ = 0; tmp$ !== cards.length; ++tmp$) {
      var element = cards[tmp$];
      var list = element.dependencies;
      addAll(destination, list);
    }
    var allDependencies = toList(toSet(plus_1(destination, dependencies)));
    var destination_0 = ArrayList_init_0(collectionSizeOrDefault(allDependencies, 10));
    var tmp$_0;
    tmp$_0 = allDependencies.iterator();
    while (tmp$_0.hasNext()) {
      var item = tmp$_0.next();
      destination_0.add_11rb$(item.addHandler_qlkmfe$(cardStack$lambda$lambda(selectCard)));
    }
    var handlers = destination_0;
    onDeinit($receiver, cardStack$lambda(allDependencies, handlers));
    ready.v = true;
    createRoot();
    selectCard();
  }
  function RGB(r, g, b) {
    RGB$Companion_getInstance();
    this.r = r;
    this.g = g;
    this.b = b;
  }
  function RGB$Companion() {
    RGB$Companion_instance = this;
  }
  RGB$Companion.prototype.create_za3lpa$ = function (value) {
    var r = value >> 16;
    var g = value >> 8 & 255;
    var b = value & 255;
    return new RGB(r, g, b);
  };
  RGB$Companion.prototype.create_61zpoe$ = function (value) {
    return this.create_za3lpa$(toInt(removePrefix(value, '#'), 16));
  };
  RGB$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var RGB$Companion_instance = null;
  function RGB$Companion_getInstance() {
    if (RGB$Companion_instance === null) {
      new RGB$Companion();
    }
    return RGB$Companion_instance;
  }
  RGB.prototype.toString = function () {
    return 'rgb(' + this.r + ', ' + this.g + ', ' + this.b + ')';
  };
  RGB.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'RGB',
    interfaces: []
  };
  RGB.prototype.component1 = function () {
    return this.r;
  };
  RGB.prototype.component2 = function () {
    return this.g;
  };
  RGB.prototype.component3 = function () {
    return this.b;
  };
  RGB.prototype.copy_qt1dr2$ = function (r, g, b) {
    return new RGB(r === void 0 ? this.r : r, g === void 0 ? this.g : g, b === void 0 ? this.b : b);
  };
  RGB.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.r) | 0;
    result = result * 31 + Kotlin.hashCode(this.g) | 0;
    result = result * 31 + Kotlin.hashCode(this.b) | 0;
    return result;
  };
  RGB.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.r, other.r) && Kotlin.equals(this.g, other.g) && Kotlin.equals(this.b, other.b)))));
  };
  function lighten($receiver, amount) {
    if (!(amount >= 0)) {
      var message = 'Failed requirement.';
      throw IllegalArgumentException_init(message.toString());
    }
    var b = $receiver.r + amount | 0;
    var tmp$ = Math_0.min(255, b);
    var b_0 = $receiver.g + amount | 0;
    var tmp$_0 = Math_0.min(255, b_0);
    var b_1 = $receiver.b + amount | 0;
    return new RGB(tmp$, tmp$_0, Math_0.min(255, b_1));
  }
  function darken($receiver, amount) {
    if (!(amount >= 0)) {
      var message = 'Failed requirement.';
      throw IllegalArgumentException_init(message.toString());
    }
    var b = $receiver.r - amount | 0;
    var tmp$ = Math_0.max(0, b);
    var b_0 = $receiver.g - amount | 0;
    var tmp$_0 = Math_0.max(0, b_0);
    var b_1 = $receiver.b - amount | 0;
    return new RGB(tmp$, tmp$_0, Math_0.max(0, b_1));
  }
  function ColorShades(base) {
    this.base = base;
    this.c100 = darken(this.base, 100);
    this.c90 = darken(this.base, 80);
    this.c80 = darken(this.base, 60);
    this.c70 = darken(this.base, 40);
    this.c60 = darken(this.base, 20);
    this.c50 = this.base;
    this.c40 = lighten(this.base, 20);
    this.c30 = lighten(this.base, 40);
    this.c20 = lighten(this.base, 60);
    this.c10 = lighten(this.base, 80);
    this.c0 = lighten(this.base, 100);
  }
  ColorShades.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'ColorShades',
    interfaces: []
  };
  ColorShades.prototype.component1 = function () {
    return this.base;
  };
  ColorShades.prototype.copy_1qjx$ = function (base) {
    return new ColorShades(base === void 0 ? this.base : base);
  };
  ColorShades.prototype.toString = function () {
    return 'ColorShades(base=' + Kotlin.toString(this.base) + ')';
  };
  ColorShades.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.base) | 0;
    return result;
  };
  ColorShades.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.base, other.base))));
  };
  function Reference(currentOrNull) {
    if (currentOrNull === void 0)
      currentOrNull = null;
    this.currentOrNull = currentOrNull;
  }
  Object.defineProperty(Reference.prototype, 'current', {
    get: function () {
      return ensureNotNull(this.currentOrNull);
    }
  });
  Reference.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Reference',
    interfaces: []
  };
  Reference.prototype.component1 = function () {
    return this.currentOrNull;
  };
  Reference.prototype.copy_11rb$ = function (currentOrNull) {
    return new Reference(currentOrNull === void 0 ? this.currentOrNull : currentOrNull);
  };
  Reference.prototype.toString = function () {
    return 'Reference(currentOrNull=' + Kotlin.toString(this.currentOrNull) + ')';
  };
  Reference.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.currentOrNull) | 0;
    return result;
  };
  Reference.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.currentOrNull, other.currentOrNull))));
  };
  var baseElement = defineInlineFunction('web2.baseElement_q99izq$', wrapFunction(function () {
    var CommonAttributes_init = _.CommonAttributes;
    var Unit = Kotlin.kotlin.Unit;
    var plus = Kotlin.kotlin.collections.plus_xfiyik$;
    var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
    var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
    var Element_0 = Element;
    var throwCCE = Kotlin.throwCCE;
    function baseElement$lambda($receiver) {
      return Unit;
    }
    return function ($receiver, tag, attrs, children) {
      if (attrs === void 0)
        attrs = new CommonAttributes_init();
      if (children === void 0)
        children = baseElement$lambda;
      var tmp$;
      var element = document.createElement(tag);
      var tmp$_0;
      tmp$_0 = attrs.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs.classes.isEmpty() || attrs.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus(attrs.classes, attrs.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs.ref != null)
        attrs.ref.currentOrNull = t;
      children(t);
      $receiver.appendChild(element);
    };
  }));
  function text($receiver, value) {
    var node = document.createTextNode(value);
    $receiver.appendChild(node);
    return node;
  }
  function on($receiver, eventName, eventHandler) {
    $receiver.addEventListener(eventName, eventHandler);
  }
  var a = defineInlineFunction('web2.a_8tcszn$', wrapFunction(function () {
    var CommonAttributes_init = _.CommonAttributes;
    var Unit = Kotlin.kotlin.Unit;
    var to = Kotlin.kotlin.to_ujzrz7$;
    var mapOf = Kotlin.kotlin.collections.mapOf_x2b85n$;
    var plus = Kotlin.kotlin.collections.plus_xfiyik$;
    var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
    var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
    var Element_0 = Element;
    var throwCCE = Kotlin.throwCCE;
    function a$lambda($receiver) {
      return Unit;
    }
    return function ($receiver, attrs, href, children) {
      if (attrs === void 0)
        attrs = new CommonAttributes_init();
      if (href === void 0)
        href = 'javascript:void(0)';
      if (children === void 0)
        children = a$lambda;
      var attrs_0 = attrs.mergeWith_zb9t9x$(mapOf(to('href', href)));
      var tmp$;
      var element = document.createElement('a');
      var tmp$_0;
      tmp$_0 = attrs_0.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs_0.classes.isEmpty() || attrs_0.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus(attrs_0.classes, attrs_0.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs_0.ref != null)
        attrs_0.ref.currentOrNull = t;
      children(t);
      $receiver.appendChild(element);
    };
  }));
  var div = defineInlineFunction('web2.div_9dg6av$', wrapFunction(function () {
    var CommonAttributes_init = _.CommonAttributes;
    var Unit = Kotlin.kotlin.Unit;
    var plus = Kotlin.kotlin.collections.plus_xfiyik$;
    var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
    var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
    var Element_0 = Element;
    var throwCCE = Kotlin.throwCCE;
    function div$lambda($receiver) {
      return Unit;
    }
    return function ($receiver, attrs, children) {
      if (attrs === void 0)
        attrs = new CommonAttributes_init();
      if (children === void 0)
        children = div$lambda;
      var tmp$;
      var element = document.createElement('div');
      var tmp$_0;
      tmp$_0 = attrs.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs.classes.isEmpty() || attrs.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus(attrs.classes, attrs.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs.ref != null)
        attrs.ref.currentOrNull = t;
      children(t);
      $receiver.appendChild(element);
    };
  }));
  var h1 = defineInlineFunction('web2.h1_m4p30b$', wrapFunction(function () {
    var CommonAttributes_init = _.CommonAttributes;
    var Unit = Kotlin.kotlin.Unit;
    var plus = Kotlin.kotlin.collections.plus_xfiyik$;
    var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
    var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
    var Element_0 = Element;
    var throwCCE = Kotlin.throwCCE;
    function h1$lambda($receiver) {
      return Unit;
    }
    return function ($receiver, attrs, children) {
      if (attrs === void 0)
        attrs = new CommonAttributes_init();
      if (children === void 0)
        children = h1$lambda;
      var tmp$;
      var element = document.createElement('h1');
      var tmp$_0;
      tmp$_0 = attrs.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs.classes.isEmpty() || attrs.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus(attrs.classes, attrs.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs.ref != null)
        attrs.ref.currentOrNull = t;
      children(t);
      $receiver.appendChild(element);
    };
  }));
  var h2 = defineInlineFunction('web2.h2_m4p30b$', wrapFunction(function () {
    var CommonAttributes_init = _.CommonAttributes;
    var Unit = Kotlin.kotlin.Unit;
    var plus = Kotlin.kotlin.collections.plus_xfiyik$;
    var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
    var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
    var Element_0 = Element;
    var throwCCE = Kotlin.throwCCE;
    function h2$lambda($receiver) {
      return Unit;
    }
    return function ($receiver, attrs, children) {
      if (attrs === void 0)
        attrs = new CommonAttributes_init();
      if (children === void 0)
        children = h2$lambda;
      var tmp$;
      var element = document.createElement('h2');
      var tmp$_0;
      tmp$_0 = attrs.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs.classes.isEmpty() || attrs.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus(attrs.classes, attrs.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs.ref != null)
        attrs.ref.currentOrNull = t;
      children(t);
      $receiver.appendChild(element);
    };
  }));
  var h3 = defineInlineFunction('web2.h3_m4p30b$', wrapFunction(function () {
    var CommonAttributes_init = _.CommonAttributes;
    var Unit = Kotlin.kotlin.Unit;
    var plus = Kotlin.kotlin.collections.plus_xfiyik$;
    var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
    var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
    var Element_0 = Element;
    var throwCCE = Kotlin.throwCCE;
    function h3$lambda($receiver) {
      return Unit;
    }
    return function ($receiver, attrs, children) {
      if (attrs === void 0)
        attrs = new CommonAttributes_init();
      if (children === void 0)
        children = h3$lambda;
      var tmp$;
      var element = document.createElement('h3');
      var tmp$_0;
      tmp$_0 = attrs.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs.classes.isEmpty() || attrs.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus(attrs.classes, attrs.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs.ref != null)
        attrs.ref.currentOrNull = t;
      children(t);
      $receiver.appendChild(element);
    };
  }));
  var h4 = defineInlineFunction('web2.h4_m4p30b$', wrapFunction(function () {
    var CommonAttributes_init = _.CommonAttributes;
    var Unit = Kotlin.kotlin.Unit;
    var plus = Kotlin.kotlin.collections.plus_xfiyik$;
    var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
    var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
    var Element_0 = Element;
    var throwCCE = Kotlin.throwCCE;
    function h4$lambda($receiver) {
      return Unit;
    }
    return function ($receiver, attrs, children) {
      if (attrs === void 0)
        attrs = new CommonAttributes_init();
      if (children === void 0)
        children = h4$lambda;
      var tmp$;
      var element = document.createElement('h4');
      var tmp$_0;
      tmp$_0 = attrs.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs.classes.isEmpty() || attrs.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus(attrs.classes, attrs.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs.ref != null)
        attrs.ref.currentOrNull = t;
      children(t);
      $receiver.appendChild(element);
    };
  }));
  var h5 = defineInlineFunction('web2.h5_m4p30b$', wrapFunction(function () {
    var CommonAttributes_init = _.CommonAttributes;
    var Unit = Kotlin.kotlin.Unit;
    var plus = Kotlin.kotlin.collections.plus_xfiyik$;
    var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
    var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
    var Element_0 = Element;
    var throwCCE = Kotlin.throwCCE;
    function h5$lambda($receiver) {
      return Unit;
    }
    return function ($receiver, attrs, children) {
      if (attrs === void 0)
        attrs = new CommonAttributes_init();
      if (children === void 0)
        children = h5$lambda;
      var tmp$;
      var element = document.createElement('h5');
      var tmp$_0;
      tmp$_0 = attrs.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs.classes.isEmpty() || attrs.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus(attrs.classes, attrs.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs.ref != null)
        attrs.ref.currentOrNull = t;
      children(t);
      $receiver.appendChild(element);
    };
  }));
  var h6 = defineInlineFunction('web2.h6_m4p30b$', wrapFunction(function () {
    var CommonAttributes_init = _.CommonAttributes;
    var Unit = Kotlin.kotlin.Unit;
    var plus = Kotlin.kotlin.collections.plus_xfiyik$;
    var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
    var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
    var Element_0 = Element;
    var throwCCE = Kotlin.throwCCE;
    function h6$lambda($receiver) {
      return Unit;
    }
    return function ($receiver, attrs, children) {
      if (attrs === void 0)
        attrs = new CommonAttributes_init();
      if (children === void 0)
        children = h6$lambda;
      var tmp$;
      var element = document.createElement('h6');
      var tmp$_0;
      tmp$_0 = attrs.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs.classes.isEmpty() || attrs.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus(attrs.classes, attrs.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs.ref != null)
        attrs.ref.currentOrNull = t;
      children(t);
      $receiver.appendChild(element);
    };
  }));
  var ul = defineInlineFunction('web2.ul_ydw3hf$', wrapFunction(function () {
    var CommonAttributes_init = _.CommonAttributes;
    var Unit = Kotlin.kotlin.Unit;
    var plus = Kotlin.kotlin.collections.plus_xfiyik$;
    var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
    var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
    var Element_0 = Element;
    var throwCCE = Kotlin.throwCCE;
    function ul$lambda($receiver) {
      return Unit;
    }
    return function ($receiver, attrs, children) {
      if (attrs === void 0)
        attrs = new CommonAttributes_init();
      if (children === void 0)
        children = ul$lambda;
      var tmp$;
      var element = document.createElement('ul');
      var tmp$_0;
      tmp$_0 = attrs.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs.classes.isEmpty() || attrs.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus(attrs.classes, attrs.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs.ref != null)
        attrs.ref.currentOrNull = t;
      children(t);
      $receiver.appendChild(element);
    };
  }));
  var li = defineInlineFunction('web2.li_nj1tw5$', wrapFunction(function () {
    var CommonAttributes_init = _.CommonAttributes;
    var Unit = Kotlin.kotlin.Unit;
    var plus = Kotlin.kotlin.collections.plus_xfiyik$;
    var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
    var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
    var Element_0 = Element;
    var throwCCE = Kotlin.throwCCE;
    function li$lambda($receiver) {
      return Unit;
    }
    return function ($receiver, attrs, children) {
      if (attrs === void 0)
        attrs = new CommonAttributes_init();
      if (children === void 0)
        children = li$lambda;
      var tmp$;
      var element = document.createElement('li');
      var tmp$_0;
      tmp$_0 = attrs.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs.classes.isEmpty() || attrs.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus(attrs.classes, attrs.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs.ref != null)
        attrs.ref.currentOrNull = t;
      children(t);
      $receiver.appendChild(element);
    };
  }));
  var form = defineInlineFunction('web2.form_yx7vw5$', wrapFunction(function () {
    var CommonAttributes_init = _.CommonAttributes;
    var Unit = Kotlin.kotlin.Unit;
    var plus = Kotlin.kotlin.collections.plus_xfiyik$;
    var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
    var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
    var Element_0 = Element;
    var throwCCE = Kotlin.throwCCE;
    function form$lambda($receiver) {
      return Unit;
    }
    return function ($receiver, attrs, children) {
      if (attrs === void 0)
        attrs = new CommonAttributes_init();
      if (children === void 0)
        children = form$lambda;
      var tmp$;
      var element = document.createElement('form');
      var tmp$_0;
      tmp$_0 = attrs.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs.classes.isEmpty() || attrs.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus(attrs.classes, attrs.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs.ref != null)
        attrs.ref.currentOrNull = t;
      children(t);
      $receiver.appendChild(element);
    };
  }));
  var input = defineInlineFunction('web2.input_x6u7o5$', wrapFunction(function () {
    var CommonAttributes_init = _.CommonAttributes;
    var Unit = Kotlin.kotlin.Unit;
    var to = Kotlin.kotlin.to_ujzrz7$;
    var mapOf = Kotlin.kotlin.collections.mapOf_qfcya0$;
    var plus = Kotlin.kotlin.collections.plus_xfiyik$;
    var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
    var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
    var Element_0 = Element;
    var throwCCE = Kotlin.throwCCE;
    function input$lambda($receiver) {
      return Unit;
    }
    return function ($receiver, attrs, placeholder, type, children) {
      if (attrs === void 0)
        attrs = new CommonAttributes_init();
      if (placeholder === void 0)
        placeholder = null;
      if (type === void 0)
        type = null;
      if (children === void 0)
        children = input$lambda;
      var attrs_0 = attrs.mergeWith_zb9t9x$(mapOf([to('placeholder', placeholder), to('type', type)]));
      var tmp$;
      var element = document.createElement('input');
      var tmp$_0;
      tmp$_0 = attrs_0.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs_0.classes.isEmpty() || attrs_0.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus(attrs_0.classes, attrs_0.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs_0.ref != null)
        attrs_0.ref.currentOrNull = t;
      children(t);
      $receiver.appendChild(element);
    };
  }));
  function WrapType(name, ordinal) {
    Enum.call(this);
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function WrapType_initFields() {
    WrapType_initFields = function () {
    };
    WrapType$soft_instance = new WrapType('soft', 0);
    WrapType$hard_instance = new WrapType('hard', 1);
  }
  var WrapType$soft_instance;
  function WrapType$soft_getInstance() {
    WrapType_initFields();
    return WrapType$soft_instance;
  }
  var WrapType$hard_instance;
  function WrapType$hard_getInstance() {
    WrapType_initFields();
    return WrapType$hard_instance;
  }
  WrapType.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'WrapType',
    interfaces: [Enum]
  };
  function WrapType$values() {
    return [WrapType$soft_getInstance(), WrapType$hard_getInstance()];
  }
  WrapType.values = WrapType$values;
  function WrapType$valueOf(name) {
    switch (name) {
      case 'soft':
        return WrapType$soft_getInstance();
      case 'hard':
        return WrapType$hard_getInstance();
      default:throwISE('No enum constant WrapType.' + name);
    }
  }
  WrapType.valueOf_61zpoe$ = WrapType$valueOf;
  var textarea = defineInlineFunction('web2.textarea_du7nhi$', wrapFunction(function () {
    var CommonAttributes_init = _.CommonAttributes;
    var Unit = Kotlin.kotlin.Unit;
    var to = Kotlin.kotlin.to_ujzrz7$;
    var mapOf = Kotlin.kotlin.collections.mapOf_qfcya0$;
    var plus = Kotlin.kotlin.collections.plus_xfiyik$;
    var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
    var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
    var Element_0 = Element;
    var throwCCE = Kotlin.throwCCE;
    function textarea$lambda($receiver) {
      return Unit;
    }
    return function ($receiver, attrs, placeholder, rows, cols, disabled, name, required, wrap, readOnly, minLength, maxLength, autoFocus, autoComplete, spellcheck, children) {
      if (attrs === void 0)
        attrs = new CommonAttributes_init();
      if (placeholder === void 0)
        placeholder = null;
      if (rows === void 0)
        rows = null;
      if (cols === void 0)
        cols = null;
      if (disabled === void 0)
        disabled = null;
      if (name === void 0)
        name = null;
      if (required === void 0)
        required = null;
      if (wrap === void 0)
        wrap = null;
      if (readOnly === void 0)
        readOnly = null;
      if (minLength === void 0)
        minLength = null;
      if (maxLength === void 0)
        maxLength = null;
      if (autoFocus === void 0)
        autoFocus = null;
      if (autoComplete === void 0)
        autoComplete = null;
      if (spellcheck === void 0)
        spellcheck = null;
      if (children === void 0)
        children = textarea$lambda;
      var tag = 'textarea';
      var attrs_0 = attrs.mergeWith_zb9t9x$(mapOf([to('placeholder', placeholder), to('rows', rows != null ? rows.toString() : null), to('cols', cols != null ? cols.toString() : null), to('disabled', disabled != null ? disabled.toString() : null), to('name', name), to('required', required != null ? required.toString() : null), to('wrap', wrap != null ? wrap.name : null), to('readonly', readOnly != null ? readOnly.toString() : null), to('minlength', minLength != null ? minLength.toString() : null), to('maxlength', maxLength != null ? maxLength.toString() : null), to('autofocus', autoFocus != null ? autoFocus.toString() : null), to('autocomplete', autoComplete != null ? autoComplete.toString() : null), to('spellcheck', spellcheck != null ? spellcheck.toString() : null)]));
      var tmp$;
      var element = document.createElement(tag);
      var tmp$_0;
      tmp$_0 = attrs_0.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name_0 = element_0.key;
        var value = element_0.value;
        element.setAttribute(name_0, value.toString());
      }
      if (!attrs_0.classes.isEmpty() || attrs_0.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus(attrs_0.classes, attrs_0.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs_0.ref != null)
        attrs_0.ref.currentOrNull = t;
      children(t);
      $receiver.appendChild(element);
    };
  }));
  var button = defineInlineFunction('web2.button_z7y0j9$', wrapFunction(function () {
    var CommonAttributes_init = _.CommonAttributes;
    var Unit = Kotlin.kotlin.Unit;
    var plus = Kotlin.kotlin.collections.plus_xfiyik$;
    var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
    var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
    var Element_0 = Element;
    var throwCCE = Kotlin.throwCCE;
    function button$lambda($receiver) {
      return Unit;
    }
    return function ($receiver, attrs, children) {
      if (attrs === void 0)
        attrs = new CommonAttributes_init();
      if (children === void 0)
        children = button$lambda;
      var tmp$;
      var element = document.createElement('button');
      var tmp$_0;
      tmp$_0 = attrs.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs.classes.isEmpty() || attrs.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus(attrs.classes, attrs.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs.ref != null)
        attrs.ref.currentOrNull = t;
      children(t);
      $receiver.appendChild(element);
    };
  }));
  function CommonAttributes(klass, classes, attributes, ref) {
    if (klass === void 0)
      klass = null;
    if (classes === void 0)
      classes = emptySet();
    if (attributes === void 0)
      attributes = emptyMap();
    if (ref === void 0)
      ref = null;
    this.klass = klass;
    this.classes = classes;
    this.attributes = attributes;
    this.ref = ref;
  }
  CommonAttributes.prototype.mergeWith_zb9t9x$ = function (additional) {
    return this.copy_s0ghyx$(void 0, void 0, this.mergeAttributes_0(additional));
  };
  CommonAttributes.prototype.mergeAttributes_0 = function (additionalAttributes) {
    var result = toMutableMap(this.attributes);
    var tmp$;
    tmp$ = additionalAttributes.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var name = element.key;
      var value = element.value;
      if (value != null) {
        result.put_xwzc9p$(name, value);
      }
    }
    return result;
  };
  CommonAttributes.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'CommonAttributes',
    interfaces: []
  };
  CommonAttributes.prototype.component1 = function () {
    return this.klass;
  };
  CommonAttributes.prototype.component2 = function () {
    return this.classes;
  };
  CommonAttributes.prototype.component3 = function () {
    return this.attributes;
  };
  CommonAttributes.prototype.component4 = function () {
    return this.ref;
  };
  CommonAttributes.prototype.copy_s0ghyx$ = function (klass, classes, attributes, ref) {
    return new CommonAttributes(klass === void 0 ? this.klass : klass, classes === void 0 ? this.classes : classes, attributes === void 0 ? this.attributes : attributes, ref === void 0 ? this.ref : ref);
  };
  CommonAttributes.prototype.toString = function () {
    return 'CommonAttributes(klass=' + Kotlin.toString(this.klass) + (', classes=' + Kotlin.toString(this.classes)) + (', attributes=' + Kotlin.toString(this.attributes)) + (', ref=' + Kotlin.toString(this.ref)) + ')';
  };
  CommonAttributes.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.klass) | 0;
    result = result * 31 + Kotlin.hashCode(this.classes) | 0;
    result = result * 31 + Kotlin.hashCode(this.attributes) | 0;
    result = result * 31 + Kotlin.hashCode(this.ref) | 0;
    return result;
  };
  CommonAttributes.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.klass, other.klass) && Kotlin.equals(this.classes, other.classes) && Kotlin.equals(this.attributes, other.attributes) && Kotlin.equals(this.ref, other.ref)))));
  };
  function onDeinit($receiver, deinit) {
    RegisteredHooks_getInstance().attachHooks_njmha2$(deinit, $receiver);
  }
  function RegisteredHooks() {
    RegisteredHooks_instance = this;
    this.hookAttribute_0 = 'data-hook';
    this.counter_0 = 0;
    this.attachedHooks_0 = HashMap_init();
  }
  RegisteredHooks.prototype.attachHooks_njmha2$ = function (hook, node) {
    var tmp$;
    try {
      var tmp$_0;
      var id = (tmp$_0 = this.counter_0, this.counter_0 = tmp$_0 + 1 | 0, tmp$_0);
      this.attachedHooks_0.put_xwzc9p$(id, hook);
      node.setAttribute(this.hookAttribute_0, id.toString());
      tmp$ = new Result(Unit);
    }
     catch (e) {
      if (Kotlin.isType(e, Throwable)) {
        tmp$ = new Result(createFailure(e));
      }
       else
        throw e;
    }
  };
  RegisteredHooks.prototype.runHooksFor_g2wvag$ = function (element, recurse) {
    if (recurse === void 0)
      recurse = true;
    var tmp$;
    try {
      var tmp$_0, tmp$_1;
      var hook = element.getAttribute(this.hookAttribute_0);
      if (recurse) {
        var children = element.querySelectorAll(':scope *[data-hook]');
        for (var idx = children.length - 1 | 0; idx >= 0; idx--) {
          this.runHooksFor_g2wvag$(Kotlin.isType(tmp$_0 = children[idx], Element) ? tmp$_0 : throwCCE(), false);
        }
      }
      if (hook != null) {
        (tmp$_1 = this.attachedHooks_0.remove_11rb$(toInt_0(hook))) != null ? tmp$_1() : null;
      }
      tmp$ = new Result(Unit);
    }
     catch (e) {
      if (Kotlin.isType(e, Throwable)) {
        tmp$ = new Result(createFailure(e));
      }
       else
        throw e;
    }
  };
  RegisteredHooks.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'RegisteredHooks',
    interfaces: []
  };
  var RegisteredHooks_instance = null;
  function RegisteredHooks_getInstance() {
    if (RegisteredHooks_instance === null) {
      new RegisteredHooks();
    }
    return RegisteredHooks_instance;
  }
  function deleteNode(node) {
    var tmp$;
    RegisteredHooks_getInstance().runHooksFor_g2wvag$(node);
    (tmp$ = node.parentElement) != null ? tmp$.removeChild(node) : null;
  }
  function flexCss$lambda($receiver) {
    set_display($receiver, 'flex');
    return Unit;
  }
  var flexCss;
  function AlignItems() {
    AlignItems_instance = this;
    this.stretch = css(AlignItems$stretch$lambda);
    this.center = css(AlignItems$center$lambda);
    this.start = css(AlignItems$start$lambda);
    this.end = css(AlignItems$end$lambda);
  }
  function AlignItems$stretch$lambda($receiver) {
    set_alignItems($receiver, 'stretch');
    return Unit;
  }
  function AlignItems$center$lambda($receiver) {
    set_alignItems($receiver, 'center');
    return Unit;
  }
  function AlignItems$start$lambda($receiver) {
    set_alignItems($receiver, 'start');
    return Unit;
  }
  function AlignItems$end$lambda($receiver) {
    set_alignItems($receiver, 'end');
    return Unit;
  }
  AlignItems.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'AlignItems',
    interfaces: []
  };
  var AlignItems_instance = null;
  function AlignItems_getInstance() {
    if (AlignItems_instance === null) {
      new AlignItems();
    }
    return AlignItems_instance;
  }
  function JustifyItems() {
    JustifyItems_instance = this;
    this.stretch = css(JustifyItems$stretch$lambda);
    this.center = css(JustifyItems$center$lambda);
    this.start = css(JustifyItems$start$lambda);
    this.end = css(JustifyItems$end$lambda);
  }
  function JustifyItems$stretch$lambda($receiver) {
    set_justifyItems($receiver, 'stretch');
    return Unit;
  }
  function JustifyItems$center$lambda($receiver) {
    set_justifyItems($receiver, 'center');
    return Unit;
  }
  function JustifyItems$start$lambda($receiver) {
    set_justifyItems($receiver, 'start');
    return Unit;
  }
  function JustifyItems$end$lambda($receiver) {
    set_justifyItems($receiver, 'end');
    return Unit;
  }
  JustifyItems.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'JustifyItems',
    interfaces: []
  };
  var JustifyItems_instance = null;
  function JustifyItems_getInstance() {
    if (JustifyItems_instance === null) {
      new JustifyItems();
    }
    return JustifyItems_instance;
  }
  function JustifyContent() {
    JustifyContent_instance = this;
    this.center = css(JustifyContent$center$lambda);
    this.start = css(JustifyContent$start$lambda);
    this.end = css(JustifyContent$end$lambda);
    this.flexStart = css(JustifyContent$flexStart$lambda);
    this.flexEnd = css(JustifyContent$flexEnd$lambda);
    this.left = css(JustifyContent$left$lambda);
    this.right = css(JustifyContent$right$lambda);
    this.normal = css(JustifyContent$normal$lambda);
    this.spaceBetween = css(JustifyContent$spaceBetween$lambda);
    this.spaceAround = css(JustifyContent$spaceAround$lambda);
    this.spaceEvenly = css(JustifyContent$spaceEvenly$lambda);
    this.stretch = css(JustifyContent$stretch$lambda);
    this.safeCenter = css(JustifyContent$safeCenter$lambda);
    this.unsafeCenter = css(JustifyContent$unsafeCenter$lambda);
  }
  function JustifyContent$center$lambda($receiver) {
    set_justifyContent($receiver, 'center');
    return Unit;
  }
  function JustifyContent$start$lambda($receiver) {
    set_justifyContent($receiver, 'start');
    return Unit;
  }
  function JustifyContent$end$lambda($receiver) {
    set_justifyContent($receiver, 'end');
    return Unit;
  }
  function JustifyContent$flexStart$lambda($receiver) {
    set_justifyContent($receiver, 'flex-start');
    return Unit;
  }
  function JustifyContent$flexEnd$lambda($receiver) {
    set_justifyContent($receiver, 'flex-end');
    return Unit;
  }
  function JustifyContent$left$lambda($receiver) {
    set_justifyContent($receiver, 'left');
    return Unit;
  }
  function JustifyContent$right$lambda($receiver) {
    set_justifyContent($receiver, 'right');
    return Unit;
  }
  function JustifyContent$normal$lambda($receiver) {
    set_justifyContent($receiver, 'normal');
    return Unit;
  }
  function JustifyContent$spaceBetween$lambda($receiver) {
    set_justifyContent($receiver, 'space-between');
    return Unit;
  }
  function JustifyContent$spaceAround$lambda($receiver) {
    set_justifyContent($receiver, 'space-around');
    return Unit;
  }
  function JustifyContent$spaceEvenly$lambda($receiver) {
    set_justifyContent($receiver, 'space-evenly');
    return Unit;
  }
  function JustifyContent$stretch$lambda($receiver) {
    set_justifyContent($receiver, 'stretch');
    return Unit;
  }
  function JustifyContent$safeCenter$lambda($receiver) {
    set_justifyContent($receiver, 'safe center');
    return Unit;
  }
  function JustifyContent$unsafeCenter$lambda($receiver) {
    set_justifyContent($receiver, 'unsafe center');
    return Unit;
  }
  JustifyContent.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'JustifyContent',
    interfaces: []
  };
  var JustifyContent_instance = null;
  function JustifyContent_getInstance() {
    if (JustifyContent_instance === null) {
      new JustifyContent();
    }
    return JustifyContent_instance;
  }
  var flex = defineInlineFunction('web2.flex_9dg6av$', wrapFunction(function () {
    var CommonAttributes_init = _.CommonAttributes;
    var setOf = Kotlin.kotlin.collections.setOf_mh5how$;
    var plus = Kotlin.kotlin.collections.plus_khz7k3$;
    var plus_0 = Kotlin.kotlin.collections.plus_xfiyik$;
    var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
    var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
    var Element_0 = Element;
    var throwCCE = Kotlin.throwCCE;
    return function ($receiver, attrs, children) {
      if (attrs === void 0)
        attrs = new CommonAttributes_init();
      var attrs_0 = attrs.copy_s0ghyx$(void 0, plus(attrs.classes, setOf(_.flexCss)));
      var div$result;
      var tmp$;
      var element = document.createElement('div');
      var tmp$_0;
      tmp$_0 = attrs_0.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs_0.classes.isEmpty() || attrs_0.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus_0(attrs_0.classes, attrs_0.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs_0.ref != null)
        attrs_0.ref.currentOrNull = t;
      children(t);
      $receiver.appendChild(element);
      return div$result;
    };
  }));
  function ListComponent(node, template) {
    this.node_0 = node;
    this.template_0 = template;
    this.backingData_0 = ArrayList_init();
  }
  Object.defineProperty(ListComponent.prototype, 'data', {
    get: function () {
      return toList(this.backingData_0);
    }
  });
  ListComponent.prototype.add_11rb$ = function (item) {
    this.backingData_0.add_11rb$(item);
    this.template_0(this.node_0, item);
  };
  ListComponent.prototype.remove_11rb$ = function (item) {
    this.removeAt_za3lpa$(this.backingData_0.indexOf_11rb$(item));
  };
  ListComponent.prototype.removeAt_za3lpa$ = function (idx) {
    var tmp$;
    if (!get_indices(this.backingData_0).contains_mef7kx$(idx)) {
      throw IllegalArgumentException_init('Index out of bounds ' + idx + ' !in 0..' + this.backingData_0.size);
    }
    this.backingData_0.removeAt_za3lpa$(idx);
    deleteNode(Kotlin.isType(tmp$ = this.node_0.childNodes[idx], Element) ? tmp$ : throwCCE());
  };
  ListComponent.prototype.clear = function () {
    this.backingData_0.clear();
    clear(this.node_0);
  };
  ListComponent.prototype.setList_4ezy5m$ = function (list) {
    this.clear();
    var tmp$;
    tmp$ = list.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      this.add_11rb$(element);
    }
  };
  ListComponent.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'ListComponent',
    interfaces: []
  };
  function list($receiver, ref, template) {
    if (ref === void 0)
      ref = new Reference();
    var listComponent = new ListComponent($receiver, template);
    ref.currentOrNull = listComponent;
    return listComponent;
  }
  function LoadingState() {
    this.loading = new BoundData(false);
    this.error = new BoundData(null);
  }
  LoadingState.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'LoadingState',
    interfaces: []
  };
  function loading$lambda(closure$state) {
    return function () {
      return closure$state.error.currentValue != null;
    };
  }
  function loading$lambda_0(closure$state) {
    return function ($receiver) {
      text($receiver, 'An error! ' + toString(closure$state.error.currentValue));
      return Unit;
    };
  }
  function loading$lambda_1(closure$state) {
    return function () {
      return closure$state.loading.currentValue;
    };
  }
  function loading$lambda_2($receiver) {
    loadingIcon($receiver);
    return Unit;
  }
  function loading$lambda_3() {
    return true;
  }
  function loading($receiver, state, children) {
    if (state === void 0)
      state = new LoadingState();
    cardStack($receiver, [new CardInStack(void 0, loading$lambda(state), loading$lambda_0(state)), new CardInStack(void 0, loading$lambda_1(state), loading$lambda_2), new CardInStack(void 0, loading$lambda_3, children)], listOf([state.loading, state.error]));
    return state;
  }
  function RemoteDataComponent() {
  }
  RemoteDataComponent.$metadata$ = {
    kind: Kind_INTERFACE,
    simpleName: 'RemoteDataComponent',
    interfaces: []
  };
  function remoteDataWithLoading$ObjectLiteral(closure$state, closure$lastLoadedData) {
    this.closure$state = closure$state;
    this.closure$lastLoadedData = closure$lastLoadedData;
  }
  function remoteDataWithLoading$ObjectLiteral$fetchData$lambda(closure$lastLoadedData) {
    return function (data) {
      closure$lastLoadedData.v = data;
      return Unit;
    };
  }
  function remoteDataWithLoading$ObjectLiteral$fetchData$lambda_0(closure$state) {
    return function (ex) {
      closure$state.error.currentValue = ex.message;
      return Unit;
    };
  }
  function remoteDataWithLoading$ObjectLiteral$fetchData$lambda_1(closure$state) {
    return function (it) {
      closure$state.loading.currentValue = false;
      return Unit;
    };
  }
  remoteDataWithLoading$ObjectLiteral.prototype.fetchData_iqa6q7$ = function (workFactory) {
    this.closure$state.loading.currentValue = true;
    workFactory().then(remoteDataWithLoading$ObjectLiteral$fetchData$lambda(this.closure$lastLoadedData)).catch(remoteDataWithLoading$ObjectLiteral$fetchData$lambda_0(this.closure$state)).then(remoteDataWithLoading$ObjectLiteral$fetchData$lambda_1(this.closure$state));
  };
  remoteDataWithLoading$ObjectLiteral.$metadata$ = {
    kind: Kind_CLASS,
    interfaces: [RemoteDataComponent]
  };
  function remoteDataWithLoading$lambda(closure$lastLoadedData, closure$children) {
    return function ($receiver) {
      var capturedData = closure$lastLoadedData.v;
      if (capturedData != null) {
        closure$children($receiver, capturedData);
      }
      return Unit;
    };
  }
  function remoteDataWithLoading($receiver, children) {
    var state = new LoadingState();
    var lastLoadedData = {v: null};
    var component = new remoteDataWithLoading$ObjectLiteral(state, lastLoadedData);
    loading($receiver, state, remoteDataWithLoading$lambda(lastLoadedData, children));
    return component;
  }
  function loadingIconStyle$lambda$lambda($receiver) {
    set_fontSize($receiver, get_pt(30));
    return Unit;
  }
  function loadingIconStyle$lambda($receiver) {
    $receiver.invoke_pt2paz$(byTag($receiver, 'h2'), loadingIconStyle$lambda$lambda);
    return Unit;
  }
  var loadingIconStyle;
  function loadingIcon$lambda$lambda(closure$numberOfDots) {
    return function () {
      closure$numberOfDots.currentValue = (closure$numberOfDots.currentValue + 1 | 0) % 5;
      return Unit;
    };
  }
  function loadingIcon$lambda$lambda_0(closure$interval) {
    return function () {
      window.clearInterval(closure$interval);
      return Unit;
    };
  }
  function loadingIcon$lambda$lambda_1(it) {
    var tmp$;
    var array = charArray(it + 1 | 0, null);
    tmp$ = array.length - 1 | 0;
    for (var i = 0; i <= tmp$; i++) {
      var value = unboxChar(toBoxedChar(46));
      array[i] = value;
    }
    return String_0(array);
  }
  function loadingIcon($receiver) {
    var attrs = new CommonAttributes(loadingIconStyle);
    var tmp$;
    var element = document.createElement('h2');
    var tmp$_0;
    tmp$_0 = attrs.attributes.entries.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      var name = element_0.key;
      var value = element_0.value;
      element.setAttribute(name, value.toString());
    }
    if (!attrs.classes.isEmpty() || attrs.klass != null) {
      element.setAttribute('class', joinToString(filterNotNull(plus_0(attrs.classes, attrs.klass)), ' '));
    }
    var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
    if (attrs.ref != null)
      attrs.ref.currentOrNull = t;
    var numberOfDots = new BoundData(0);
    var interval = window.setInterval(loadingIcon$lambda$lambda(numberOfDots), 250);
    onDeinit(t, loadingIcon$lambda$lambda_0(interval));
    boundText(t, numberOfDots, loadingIcon$lambda$lambda_1);
    $receiver.appendChild(element);
  }
  function reset$lambda$lambda$lambda($receiver) {
    set_margin($receiver, get_px(0));
    set_padding($receiver, get_px(0));
    return Unit;
  }
  function reset$lambda$lambda$lambda_0($receiver) {
    set_fontWeight($receiver, 'normal');
    return Unit;
  }
  function reset$lambda$lambda($receiver) {
    set_listStyle($receiver, 'none');
    return Unit;
  }
  function reset$lambda$lambda$lambda_1($receiver) {
    set_margin($receiver, get_px(0));
    return Unit;
  }
  function reset$lambda$lambda_0($receiver) {
    set_boxSizing($receiver, 'border-box');
    return Unit;
  }
  function reset$lambda$lambda_1($receiver) {
    set_boxSizing($receiver, 'inherit');
    return Unit;
  }
  function reset$lambda$lambda_2($receiver) {
    set_border($receiver, '0');
    return Unit;
  }
  function reset$lambda$lambda_3($receiver) {
    set_borderCollapse($receiver, 'collapse');
    set_borderSpacing($receiver, '0');
    return Unit;
  }
  function reset$lambda$lambda_4($receiver) {
    set_padding($receiver, '0');
    set_textAlign($receiver, 'left');
    return Unit;
  }
  function reset$lambda($receiver) {
    var tmp$;
    tmp$ = listOf(['html', 'body', 'p', 'ol', 'ul', 'li', 'dl', 'dt', 'dd', 'blockquote', 'figure', 'fieldset', 'legend', 'textarea', 'pre', 'iframe', 'hr', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6']).iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      $receiver.invoke_pt2paz$(byTag($receiver, element), reset$lambda$lambda$lambda);
    }
    var tmp$_0;
    tmp$_0 = (new IntRange(1, 6)).iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      $receiver.invoke_pt2paz$(byTag($receiver, 'h' + element_0), reset$lambda$lambda$lambda_0);
    }
    $receiver.invoke_pt2paz$(byTag($receiver, 'ul'), reset$lambda$lambda);
    var tmp$_1;
    tmp$_1 = listOf(['button', 'input', 'select', 'textarea']).iterator();
    while (tmp$_1.hasNext()) {
      var element_1 = tmp$_1.next();
      $receiver.invoke_pt2paz$(byTag($receiver, element_1), reset$lambda$lambda$lambda_1);
    }
    $receiver.invoke_pt2paz$(byTag($receiver, 'html'), reset$lambda$lambda_0);
    $receiver.invoke_pt2paz$(or(or(matchAny($receiver), withPseudoClass(matchAny($receiver), 'before')), withPseudoClass(matchAny($receiver), 'after')), reset$lambda$lambda_1);
    var tmp$_2;
    tmp$_2 = listOf(['img', 'video']).iterator();
    while (tmp$_2.hasNext()) {
      var element_2 = tmp$_2.next();
      set_height($receiver, 'auto');
      set_maxWidth($receiver, get_percent(100));
    }
    $receiver.invoke_pt2paz$(byTag($receiver, 'iframe'), reset$lambda$lambda_2);
    $receiver.invoke_pt2paz$(byTag($receiver, 'table'), reset$lambda$lambda_3);
    $receiver.invoke_pt2paz$(or(byTag($receiver, 'td'), byTag($receiver, 'th')), reset$lambda$lambda_4);
    return Unit;
  }
  var reset;
  var routeLink = defineInlineFunction('web2.routeLink_8tcszn$', wrapFunction(function () {
    var CommonAttributes_init = _.CommonAttributes;
    var Unit = Kotlin.kotlin.Unit;
    var on = _.on_z3ui9j$;
    var to = Kotlin.kotlin.to_ujzrz7$;
    var mapOf = Kotlin.kotlin.collections.mapOf_x2b85n$;
    var plus = Kotlin.kotlin.collections.plus_xfiyik$;
    var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
    var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
    var Element_0 = Element;
    var throwCCE = Kotlin.throwCCE;
    function routeLink$lambda($receiver) {
      return Unit;
    }
    function routeLink$lambda$lambda(closure$href) {
      return function (event) {
        event.preventDefault();
        _.Router.push_61zpoe$(closure$href);
        return Unit;
      };
    }
    return function ($receiver, attrs, href, children) {
      if (attrs === void 0)
        attrs = new CommonAttributes_init();
      if (children === void 0)
        children = routeLink$lambda;
      var attrs_0 = attrs.mergeWith_zb9t9x$(mapOf(to('href', href)));
      var tmp$;
      var element = document.createElement('a');
      var tmp$_0;
      tmp$_0 = attrs_0.attributes.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var name = element_0.key;
        var value = element_0.value;
        element.setAttribute(name, value.toString());
      }
      if (!attrs_0.classes.isEmpty() || attrs_0.klass != null) {
        element.setAttribute('class', joinToString(filterNotNull(plus(attrs_0.classes, attrs_0.klass)), ' '));
      }
      var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
      if (attrs_0.ref != null)
        attrs_0.ref.currentOrNull = t;
      children(t);
      on(t, 'click', routeLink$lambda$lambda(href));
      $receiver.appendChild(element);
    };
  }));
  function Router() {
    Router_instance = this;
    this.routes_0 = ArrayList_init();
    this.rootNode_w5pea7$_0 = this.rootNode_w5pea7$_0;
    this.currentRouteNode_e43u4d$_0 = this.currentRouteNode_e43u4d$_0;
    this.notFoundRoute_0 = Router$notFoundRoute$lambda;
  }
  function Router$RouteWithGenerator(route, generator) {
    this.route = route;
    this.generator = generator;
  }
  Router$RouteWithGenerator.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'RouteWithGenerator',
    interfaces: []
  };
  Router$RouteWithGenerator.prototype.component1 = function () {
    return this.route;
  };
  Router$RouteWithGenerator.prototype.component2 = function () {
    return this.generator;
  };
  Router$RouteWithGenerator.prototype.copy_6sqyq3$ = function (route, generator) {
    return new Router$RouteWithGenerator(route === void 0 ? this.route : route, generator === void 0 ? this.generator : generator);
  };
  Router$RouteWithGenerator.prototype.toString = function () {
    return 'RouteWithGenerator(route=' + Kotlin.toString(this.route) + (', generator=' + Kotlin.toString(this.generator)) + ')';
  };
  Router$RouteWithGenerator.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.route) | 0;
    result = result * 31 + Kotlin.hashCode(this.generator) | 0;
    return result;
  };
  Router$RouteWithGenerator.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.route, other.route) && Kotlin.equals(this.generator, other.generator)))));
  };
  Object.defineProperty(Router.prototype, 'rootNode_0', {
    get: function () {
      if (this.rootNode_w5pea7$_0 == null)
        return throwUPAE('rootNode');
      return this.rootNode_w5pea7$_0;
    },
    set: function (rootNode) {
      this.rootNode_w5pea7$_0 = rootNode;
    }
  });
  Object.defineProperty(Router.prototype, 'currentRouteNode_0', {
    get: function () {
      if (this.currentRouteNode_e43u4d$_0 == null)
        return throwUPAE('currentRouteNode');
      return this.currentRouteNode_e43u4d$_0;
    },
    set: function (currentRouteNode) {
      this.currentRouteNode_e43u4d$_0 = currentRouteNode;
    }
  });
  Router.prototype.mount_2rdptt$ = function (node) {
    this.initializePopStateListener_0(node);
    this.rootNode_0 = node;
    this.mountRouteNode_0();
  };
  Router.prototype.mountRouteNode_0 = function () {
    var ref = new Reference();
    var $receiver = this.rootNode_0;
    var attrs = new CommonAttributes(void 0, void 0, void 0, ref);
    var tmp$;
    var element = document.createElement('div');
    var tmp$_0;
    tmp$_0 = attrs.attributes.entries.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      var name = element_0.key;
      var value = element_0.value;
      element.setAttribute(name, value.toString());
    }
    if (!attrs.classes.isEmpty() || attrs.klass != null) {
      element.setAttribute('class', joinToString(filterNotNull(plus_0(attrs.classes, attrs.klass)), ' '));
    }
    var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
    if (attrs.ref != null)
      attrs.ref.currentOrNull = t;
    $receiver.appendChild(element);
    this.currentRouteNode_0 = ref.current;
    return ref.current;
  };
  Router.prototype.push_61zpoe$ = function (url) {
    window.history.pushState(null, '', url);
    this.refresh();
  };
  Router.prototype.refresh = function () {
    var tmp$, tmp$_0;
    var path = window.location.pathname;
    var $receiver = split(path, ['/']);
    var destination = ArrayList_init();
    var tmp$_1;
    tmp$_1 = $receiver.iterator();
    while (tmp$_1.hasNext()) {
      var element = tmp$_1.next();
      if (element.length > 0)
        destination.add_11rb$(element);
    }
    var segments = destination;
    var $receiver_0 = this.routes_0;
    var destination_0 = ArrayList_init();
    var tmp$_2;
    tmp$_2 = $receiver_0.iterator();
    while (tmp$_2.hasNext()) {
      var element_0 = tmp$_2.next();
      if (element_0.route.segments.size <= segments.size)
        destination_0.add_11rb$(element_0);
    }
    var eligibleRoutes = {v: destination_0};
    var tmp$_3, tmp$_0_0;
    var index = 0;
    tmp$_3 = segments.iterator();
    while (tmp$_3.hasNext()) {
      var item = tmp$_3.next();
      var index_0 = checkIndexOverflow((tmp$_0_0 = index, index = tmp$_0_0 + 1 | 0, tmp$_0_0));
      var $receiver_1 = eligibleRoutes.v;
      var destination_1 = ArrayList_init();
      var tmp$_4;
      tmp$_4 = $receiver_1.iterator();
      loop_label: while (tmp$_4.hasNext()) {
        var element_1 = tmp$_4.next();
        var predicate$result;
        predicate$break: do {
          var route = element_1.component1();
          var tmp$_5;
          tmp$_5 = getOrNull(route.segments, index_0);
          if (tmp$_5 == null) {
            predicate$result = equals(lastOrNull(route.segments), RouteSegment$Remaining_getInstance());
            break predicate$break;
          }
          var routeSegment = tmp$_5;
          if (Kotlin.isType(routeSegment, RouteSegment$Plain)) {
            predicate$result = equals(routeSegment.segment, item);
          }
           else if (equals(routeSegment, RouteSegment$Remaining_getInstance())) {
            predicate$result = true;
          }
           else if (equals(routeSegment, RouteSegment$Wildcard_getInstance())) {
            predicate$result = true;
          }
           else {
            predicate$result = Kotlin.noWhenBranchMatched();
          }
        }
         while (false);
        if (predicate$result)
          destination_1.add_11rb$(element_1);
      }
      eligibleRoutes.v = destination_1;
    }
    if (eligibleRoutes.v.size > 1) {
      console.warn('Found more than one eligible route!', eligibleRoutes.v);
      console.warn('The first route will be chosen');
    }
    var generator = (tmp$_0 = (tmp$ = firstOrNull(eligibleRoutes.v)) != null ? tmp$.generator : null) != null ? tmp$_0 : this.notFoundRoute_0;
    deleteNode(this.currentRouteNode_0);
    generator(this.mountRouteNode_0());
  };
  Router.prototype.route_18ee83$ = function (route, children) {
    var tmp$ = this.routes_0;
    var $receiver = new RouteBuilder();
    route($receiver);
    tmp$.add_11rb$(new Router$RouteWithGenerator($receiver.build(), children));
  };
  function Router$initializePopStateListener$lambda(this$Router) {
    return function (event) {
      this$Router.refresh();
      return Unit;
    };
  }
  function Router$initializePopStateListener$lambda_0(closure$onPopState) {
    return function () {
      window.removeEventListener('popstate', closure$onPopState);
      return Unit;
    };
  }
  Router.prototype.initializePopStateListener_0 = function ($receiver) {
    var onPopState = Router$initializePopStateListener$lambda(this);
    window.addEventListener('popstate', onPopState);
    onDeinit($receiver, Router$initializePopStateListener$lambda_0(onPopState));
  };
  function Router$notFoundRoute$lambda($receiver) {
    var attrs;
    attrs = new CommonAttributes();
    var tmp$;
    var element = document.createElement('div');
    var tmp$_0;
    tmp$_0 = attrs.attributes.entries.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      var name = element_0.key;
      var value = element_0.value;
      element.setAttribute(name, value.toString());
    }
    if (!attrs.classes.isEmpty() || attrs.klass != null) {
      element.setAttribute('class', joinToString(filterNotNull(plus_0(attrs.classes, attrs.klass)), ' '));
    }
    var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
    if (attrs.ref != null)
      attrs.ref.currentOrNull = t;
    text(t, 'Not found');
    $receiver.appendChild(element);
    return Unit;
  }
  Router.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Router',
    interfaces: []
  };
  var Router_instance = null;
  function Router_getInstance() {
    if (Router_instance === null) {
      new Router();
    }
    return Router_instance;
  }
  function router($receiver, block) {
    Router_getInstance().mount_2rdptt$($receiver);
    block(Router_getInstance());
    Router_getInstance().refresh();
  }
  function Route(segments) {
    this.segments = segments;
  }
  Route.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Route',
    interfaces: []
  };
  Route.prototype.component1 = function () {
    return this.segments;
  };
  Route.prototype.copy_y45pwh$ = function (segments) {
    return new Route(segments === void 0 ? this.segments : segments);
  };
  Route.prototype.toString = function () {
    return 'Route(segments=' + Kotlin.toString(this.segments) + ')';
  };
  Route.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.segments) | 0;
    return result;
  };
  Route.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.segments, other.segments))));
  };
  function RouteSegment() {
  }
  function RouteSegment$Plain(segment) {
    RouteSegment.call(this);
    this.segment = segment;
  }
  RouteSegment$Plain.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Plain',
    interfaces: [RouteSegment]
  };
  RouteSegment$Plain.prototype.component1 = function () {
    return this.segment;
  };
  RouteSegment$Plain.prototype.copy_61zpoe$ = function (segment) {
    return new RouteSegment$Plain(segment === void 0 ? this.segment : segment);
  };
  RouteSegment$Plain.prototype.toString = function () {
    return 'Plain(segment=' + Kotlin.toString(this.segment) + ')';
  };
  RouteSegment$Plain.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.segment) | 0;
    return result;
  };
  RouteSegment$Plain.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.segment, other.segment))));
  };
  function RouteSegment$Remaining() {
    RouteSegment$Remaining_instance = this;
    RouteSegment.call(this);
  }
  RouteSegment$Remaining.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Remaining',
    interfaces: [RouteSegment]
  };
  var RouteSegment$Remaining_instance = null;
  function RouteSegment$Remaining_getInstance() {
    if (RouteSegment$Remaining_instance === null) {
      new RouteSegment$Remaining();
    }
    return RouteSegment$Remaining_instance;
  }
  function RouteSegment$Wildcard() {
    RouteSegment$Wildcard_instance = this;
    RouteSegment.call(this);
  }
  RouteSegment$Wildcard.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Wildcard',
    interfaces: [RouteSegment]
  };
  var RouteSegment$Wildcard_instance = null;
  function RouteSegment$Wildcard_getInstance() {
    if (RouteSegment$Wildcard_instance === null) {
      new RouteSegment$Wildcard();
    }
    return RouteSegment$Wildcard_instance;
  }
  RouteSegment.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'RouteSegment',
    interfaces: []
  };
  function RouteBuilder() {
    this.segments_0 = ArrayList_init();
  }
  RouteBuilder.prototype.unaryPlus_pdl1vz$ = function ($receiver) {
    this.segments_0.add_11rb$(new RouteSegment$Plain($receiver));
  };
  RouteBuilder.prototype.remaining = function () {
    this.segments_0.add_11rb$(RouteSegment$Remaining_getInstance());
  };
  RouteBuilder.prototype.wildcard = function () {
    this.segments_0.add_11rb$(RouteSegment$Wildcard_getInstance());
  };
  RouteBuilder.prototype.build = function () {
    return new Route(this.segments_0);
  };
  RouteBuilder.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'RouteBuilder',
    interfaces: []
  };
  function ToastType(name, ordinal) {
    Enum.call(this);
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function ToastType_initFields() {
    ToastType_initFields = function () {
    };
    ToastType$INFO_instance = new ToastType('INFO', 0);
  }
  var ToastType$INFO_instance;
  function ToastType$INFO_getInstance() {
    ToastType_initFields();
    return ToastType$INFO_instance;
  }
  ToastType.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'ToastType',
    interfaces: [Enum]
  };
  function ToastType$values() {
    return [ToastType$INFO_getInstance()];
  }
  ToastType.values = ToastType$values;
  function ToastType$valueOf(name) {
    switch (name) {
      case 'INFO':
        return ToastType$INFO_getInstance();
      default:throwISE('No enum constant ToastType.' + name);
    }
  }
  ToastType.valueOf_61zpoe$ = ToastType$valueOf;
  function Toast(type, message, duration) {
    this.type = type;
    this.message = message;
    this.duration = duration;
  }
  Toast.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Toast',
    interfaces: []
  };
  Toast.prototype.component1 = function () {
    return this.type;
  };
  Toast.prototype.component2 = function () {
    return this.message;
  };
  Toast.prototype.component3 = function () {
    return this.duration;
  };
  Toast.prototype.copy_m0hdow$ = function (type, message, duration) {
    return new Toast(type === void 0 ? this.type : type, message === void 0 ? this.message : message, duration === void 0 ? this.duration : duration);
  };
  Toast.prototype.toString = function () {
    return 'Toast(type=' + Kotlin.toString(this.type) + (', message=' + Kotlin.toString(this.message)) + (', duration=' + Kotlin.toString(this.duration)) + ')';
  };
  Toast.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.type) | 0;
    result = result * 31 + Kotlin.hashCode(this.message) | 0;
    result = result * 31 + Kotlin.hashCode(this.duration) | 0;
    return result;
  };
  Toast.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.type, other.type) && Kotlin.equals(this.message, other.message) && Kotlin.equals(this.duration, other.duration)))));
  };
  function Toasts() {
    Toasts_instance = this;
    this.subscribers_0 = ArrayList_init();
  }
  Toasts.prototype.subscribe_8cpcba$ = function (subscriber) {
    this.subscribers_0.add_11rb$(subscriber);
    return subscriber;
  };
  Toasts.prototype.unsubscribe_8cpcba$ = function (subscriber) {
    this.subscribers_0.remove_11rb$(subscriber);
  };
  Toasts.prototype.push_1c7o5j$ = function (toast) {
    var tmp$;
    tmp$ = this.subscribers_0.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      element(toast);
    }
  };
  Toasts.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Toasts',
    interfaces: []
  };
  var Toasts_instance = null;
  function Toasts_getInstance() {
    if (Toasts_instance === null) {
      new Toasts();
    }
    return Toasts_instance;
  }
  var TOAST_ACTIVE;
  function toastStyle$lambda$lambda($receiver) {
    set_bottom($receiver, get_px(30));
    set_opacity($receiver, '1');
    return Unit;
  }
  function toastStyle$lambda($receiver) {
    set_position($receiver, 'fixed');
    set_bottom($receiver, get_px(-80));
    set_left($receiver, get_px(30));
    set_minWidth($receiver, get_px(300));
    set_maxWidth($receiver, get_px(500));
    set_height($receiver, get_px(60));
    set_backgroundColor($receiver, 'black');
    set_color($receiver, 'white');
    set_margin($receiver, get_px(10));
    set_transition($receiver, 'opacity 0.5s ease-in, bottom 0.5s ease-in');
    set_opacity($receiver, '0');
    $receiver.invoke_pt2paz$(and(matchSelf($receiver), byClass($receiver, TOAST_ACTIVE)), toastStyle$lambda$lambda);
    return Unit;
  }
  var toastStyle;
  function toasts$lambda$lambda(it) {
    return it == null ? emptySet() : setOf_0(TOAST_ACTIVE);
  }
  function toasts$lambda$lambda_0(closure$queue) {
    return function (it) {
      closure$queue.add_11rb$(it);
      return Unit;
    };
  }
  function toasts$lambda$lambda_1(closure$nextDeadline, closure$queue, closure$activeToast) {
    return function () {
      var now = Date.now();
      if (now >= closure$nextDeadline.v) {
        if (!closure$queue.isEmpty()) {
          var nextToast = closure$queue.removeAt_za3lpa$(0);
          closure$activeToast.currentValue = nextToast;
          closure$nextDeadline.v = nextToast.duration.toNumber() + now;
        }
         else if (closure$activeToast.currentValue != null) {
          closure$activeToast.currentValue = null;
        }
      }
      return Unit;
    };
  }
  function toasts$lambda$lambda_2(closure$subscription) {
    return function () {
      Toasts_getInstance().unsubscribe_8cpcba$(closure$subscription);
      return Unit;
    };
  }
  function toasts$lambda$lambda_3(closure$interval) {
    return function () {
      window.clearInterval(closure$interval);
      return Unit;
    };
  }
  function toasts$lambda$lambda_4(toast) {
    var tmp$;
    return (tmp$ = toast != null ? toast.message : null) != null ? tmp$ : '';
  }
  function toasts($receiver) {
    var activeToast = new BoundData(null);
    var nextDeadline = {v: 0.0};
    var queue = ArrayList_init();
    var attrs = new CommonAttributes(void 0, setOf([AlignItems_getInstance().center, JustifyContent_getInstance().center, toastStyle]));
    var attrs_0 = attrs.copy_s0ghyx$(void 0, plus(attrs.classes, setOf_0(_.flexCss)));
    var tmp$;
    var element = document.createElement('div');
    var tmp$_0;
    tmp$_0 = attrs_0.attributes.entries.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      var name = element_0.key;
      var value = element_0.value;
      element.setAttribute(name, value.toString());
    }
    if (!attrs_0.classes.isEmpty() || attrs_0.klass != null) {
      element.setAttribute('class', joinToString(filterNotNull(plus_0(attrs_0.classes, attrs_0.klass)), ' '));
    }
    var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
    if (attrs_0.ref != null)
      attrs_0.ref.currentOrNull = t;
    boundClass(t, activeToast, void 0, toasts$lambda$lambda);
    var subscription = Toasts_getInstance().subscribe_8cpcba$(toasts$lambda$lambda_0(queue));
    var interval = window.setInterval(toasts$lambda$lambda_1(nextDeadline, queue, activeToast), 100);
    onDeinit(t, toasts$lambda$lambda_2(subscription));
    onDeinit(t, toasts$lambda$lambda_3(interval));
    boundText(t, activeToast, toasts$lambda$lambda_4);
    $receiver.appendChild(element);
  }
  function Course(name) {
    this.name = name;
  }
  Course.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Course',
    interfaces: []
  };
  Course.prototype.component1 = function () {
    return this.name;
  };
  Course.prototype.copy_61zpoe$ = function (name) {
    return new Course(name === void 0 ? this.name : name);
  };
  Course.prototype.toString = function () {
    return 'Course(name=' + Kotlin.toString(this.name) + ')';
  };
  Course.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.name) | 0;
    return result;
  };
  Course.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.name, other.name))));
  };
  function CoursesBackend() {
    CoursesBackend_instance = this;
    RPCNamespace.call(this, 'courses');
    this.list = this.rpc_26bn4z$('list');
  }
  CoursesBackend.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'CoursesBackend',
    interfaces: [RPCNamespace]
  };
  var CoursesBackend_instance = null;
  function CoursesBackend_getInstance() {
    if (CoursesBackend_instance === null) {
      new CoursesBackend();
    }
    return CoursesBackend_instance;
  }
  function container$lambda($receiver) {
    set_width($receiver, get_px(900));
    set_margin($receiver, '0 auto');
    return Unit;
  }
  var container;
  function coursesSurface$lambda($receiver) {
    set_width($receiver, get_px(500));
    set_height($receiver, get_px(300));
    return Unit;
  }
  var coursesSurface;
  function courses$lambda$lambda$lambda($receiver, course) {
    var attrs;
    attrs = new CommonAttributes();
    var tmp$;
    var element = document.createElement('div');
    var tmp$_0;
    tmp$_0 = attrs.attributes.entries.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      var name = element_0.key;
      var value = element_0.value;
      element.setAttribute(name, value.toString());
    }
    if (!attrs.classes.isEmpty() || attrs.klass != null) {
      element.setAttribute('class', joinToString(filterNotNull(plus_0(attrs.classes, attrs.klass)), ' '));
    }
    var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
    if (attrs.ref != null)
      attrs.ref.currentOrNull = t;
    text(t, 'Course');
    text(t, course.name);
    $receiver.appendChild(element);
    return Unit;
  }
  function courses$lambda$lambda$lambda_0(closure$listComponent) {
    return function ($receiver, data) {
      closure$listComponent.setList_4ezy5m$(data);
      return Unit;
    };
  }
  function courses$lambda$lambda$lambda_1() {
    return call(CoursesBackend_getInstance().list, Unit);
  }
  function courses$lambda$lambda($receiver) {
    var listComponent = list($receiver, void 0, courses$lambda$lambda$lambda);
    var remoteDataComponent = remoteDataWithLoading($receiver, courses$lambda$lambda$lambda_0(listComponent));
    remoteDataComponent.fetchData_iqa6q7$(courses$lambda$lambda$lambda_1);
    return Unit;
  }
  function courses($receiver) {
    Header_getInstance().activePage.currentValue = Page$COURSES_getInstance();
    var attrs = new CommonAttributes(container);
    var tmp$;
    var element = document.createElement('div');
    var tmp$_0;
    tmp$_0 = attrs.attributes.entries.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      var name = element_0.key;
      var value = element_0.value;
      element.setAttribute(name, value.toString());
    }
    if (!attrs.classes.isEmpty() || attrs.klass != null) {
      element.setAttribute('class', joinToString(filterNotNull(plus_0(attrs.classes, attrs.klass)), ' '));
    }
    var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
    if (attrs.ref != null)
      attrs.ref.currentOrNull = t;
    text(t, 'Courses!');
    surface(t, new CommonAttributes(coursesSurface), 1, courses$lambda$lambda);
    $receiver.appendChild(element);
  }
  function routeLink$lambda$lambda(closure$href) {
    return function (event) {
      event.preventDefault();
      _.Router.push_61zpoe$(closure$href);
      return Unit;
    };
  }
  var ACTIVE_PAGE_CLASS;
  function style$lambda$lambda($receiver) {
    set_fontSize($receiver, get_pt(20));
    set_marginRight($receiver, get_px(100));
    return Unit;
  }
  function style$lambda$lambda_0($receiver) {
    set_color($receiver, Theme_getInstance().onPrimary.toString());
    set_textDecoration($receiver, 'none');
    set_marginRight($receiver, get_px(20));
    set_outline($receiver, '0');
    return Unit;
  }
  function style$lambda$lambda_1($receiver) {
    set_opacity($receiver, '1');
    return Unit;
  }
  function style$lambda$lambda_2($receiver) {
    set_opacity($receiver, '0');
    set_content($receiver, "''");
    set_display($receiver, 'inline-block');
    set_width($receiver, get_percent(100));
    set_marginRight($receiver, get_percent(-100));
    set_height($receiver, get_px(2));
    set_backgroundColor($receiver, Theme_getInstance().onPrimary.toString());
    set_position($receiver, 'relative');
    set_top($receiver, get_px(12));
    set_transition($receiver, 'opacity 0.25s ease-in');
    return Unit;
  }
  function style$lambda$lambda_3($receiver) {
    set_opacity($receiver, '1');
    return Unit;
  }
  function style$lambda($receiver) {
    set_height($receiver, get_px(80));
    set_backgroundColor($receiver, Theme_getInstance().primary.base.toString());
    set_color($receiver, Theme_getInstance().onPrimary.toString());
    set_paddingLeft($receiver, get_px(16));
    set_display($receiver, 'flex');
    set_alignItems($receiver, 'center');
    set_flexDirection($receiver, 'row');
    $receiver.invoke_pt2paz$(byTag($receiver, 'h1'), style$lambda$lambda);
    $receiver.invoke_pt2paz$(byTag($receiver, 'a'), style$lambda$lambda_0);
    $receiver.invoke_pt2paz$(withPseudoElement(and(byTag($receiver, 'a'), byClass($receiver, ACTIVE_PAGE_CLASS)), 'before'), style$lambda$lambda_1);
    $receiver.invoke_pt2paz$(withPseudoElement(byTag($receiver, 'a'), 'before'), style$lambda$lambda_2);
    $receiver.invoke_pt2paz$(withPseudoElement(withPseudoClass(byTag($receiver, 'a'), 'hover'), 'before'), style$lambda$lambda_3);
    return Unit;
  }
  var style;
  function Page(name, ordinal) {
    Enum.call(this);
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function Page_initFields() {
    Page_initFields = function () {
    };
    Page$HOME_instance = new Page('HOME', 0);
    Page$COURSES_instance = new Page('COURSES', 1);
    Page$CALENDAR_instance = new Page('CALENDAR', 2);
  }
  var Page$HOME_instance;
  function Page$HOME_getInstance() {
    Page_initFields();
    return Page$HOME_instance;
  }
  var Page$COURSES_instance;
  function Page$COURSES_getInstance() {
    Page_initFields();
    return Page$COURSES_instance;
  }
  var Page$CALENDAR_instance;
  function Page$CALENDAR_getInstance() {
    Page_initFields();
    return Page$CALENDAR_instance;
  }
  Page.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Page',
    interfaces: [Enum]
  };
  function Page$values() {
    return [Page$HOME_getInstance(), Page$COURSES_getInstance(), Page$CALENDAR_getInstance()];
  }
  Page.values = Page$values;
  function Page$valueOf(name) {
    switch (name) {
      case 'HOME':
        return Page$HOME_getInstance();
      case 'COURSES':
        return Page$COURSES_getInstance();
      case 'CALENDAR':
        return Page$CALENDAR_getInstance();
      default:throwISE('No enum constant edu.Page.' + name);
    }
  }
  Page.valueOf_61zpoe$ = Page$valueOf;
  function Header() {
    Header_instance = this;
    this.activePage = new BoundData(Page$HOME_getInstance());
  }
  Header.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Header',
    interfaces: []
  };
  var Header_instance = null;
  function Header_getInstance() {
    if (Header_instance === null) {
      new Header();
    }
    return Header_instance;
  }
  function header$lambda$lambda$lambda(it) {
    return it === Page$HOME_getInstance();
  }
  function header$lambda$lambda$lambda_0(it) {
    return it === Page$COURSES_getInstance();
  }
  function header$lambda$lambda$lambda_1(it) {
    return it === Page$CALENDAR_getInstance();
  }
  function header($receiver) {
    var attrs = new CommonAttributes(style);
    var tmp$;
    var element = document.createElement('div');
    var tmp$_0;
    tmp$_0 = attrs.attributes.entries.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      var name = element_0.key;
      var value = element_0.value;
      element.setAttribute(name, value.toString());
    }
    if (!attrs.classes.isEmpty() || attrs.klass != null) {
      element.setAttribute('class', joinToString(filterNotNull(plus_0(attrs.classes, attrs.klass)), ' '));
    }
    var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
    if (attrs.ref != null)
      attrs.ref.currentOrNull = t;
    var attrs_0;
    attrs_0 = new CommonAttributes();
    var tmp$_1;
    var element_1 = document.createElement('h1');
    var tmp$_2;
    tmp$_2 = attrs_0.attributes.entries.iterator();
    while (tmp$_2.hasNext()) {
      var element_2 = tmp$_2.next();
      var name_0 = element_2.key;
      var value_0 = element_2.value;
      element_1.setAttribute(name_0, value_0.toString());
    }
    if (!attrs_0.classes.isEmpty() || attrs_0.klass != null) {
      element_1.setAttribute('class', joinToString(filterNotNull(plus_0(attrs_0.classes, attrs_0.klass)), ' '));
    }
    var t_0 = Kotlin.isType(tmp$_1 = element_1, Element_0) ? tmp$_1 : throwCCE();
    if (attrs_0.ref != null)
      attrs_0.ref.currentOrNull = t_0;
    var attrs_1 = (new CommonAttributes()).mergeWith_zb9t9x$(mapOf(to('href', '/')));
    var tmp$_3;
    var element_3 = document.createElement('a');
    var tmp$_4;
    tmp$_4 = attrs_1.attributes.entries.iterator();
    while (tmp$_4.hasNext()) {
      var element_4 = tmp$_4.next();
      var name_1 = element_4.key;
      var value_1 = element_4.value;
      element_3.setAttribute(name_1, value_1.toString());
    }
    if (!attrs_1.classes.isEmpty() || attrs_1.klass != null) {
      element_3.setAttribute('class', joinToString(filterNotNull(plus_0(attrs_1.classes, attrs_1.klass)), ' '));
    }
    var t_1 = Kotlin.isType(tmp$_3 = element_3, Element_0) ? tmp$_3 : throwCCE();
    if (attrs_1.ref != null)
      attrs_1.ref.currentOrNull = t_1;
    text(t_1, 'Board');
    on(t_1, 'click', routeLink$lambda$lambda('/'));
    t_0.appendChild(element_3);
    t.appendChild(element_1);
    var attrs_2 = (new CommonAttributes()).mergeWith_zb9t9x$(mapOf(to('href', '/')));
    var tmp$_5;
    var element_5 = document.createElement('a');
    var tmp$_6;
    tmp$_6 = attrs_2.attributes.entries.iterator();
    while (tmp$_6.hasNext()) {
      var element_6 = tmp$_6.next();
      var name_2 = element_6.key;
      var value_2 = element_6.value;
      element_5.setAttribute(name_2, value_2.toString());
    }
    if (!attrs_2.classes.isEmpty() || attrs_2.klass != null) {
      element_5.setAttribute('class', joinToString(filterNotNull(plus_0(attrs_2.classes, attrs_2.klass)), ' '));
    }
    var t_2 = Kotlin.isType(tmp$_5 = element_5, Element_0) ? tmp$_5 : throwCCE();
    if (attrs_2.ref != null)
      attrs_2.ref.currentOrNull = t_2;
    boundClassByPredicate(t_2, Header_getInstance().activePage, [ACTIVE_PAGE_CLASS], void 0, header$lambda$lambda$lambda);
    text(t_2, 'Home');
    on(t_2, 'click', routeLink$lambda$lambda('/'));
    t.appendChild(element_5);
    var href = '/courses';
    var attrs_3 = (new CommonAttributes()).mergeWith_zb9t9x$(mapOf(to('href', href)));
    var tmp$_7;
    var element_7 = document.createElement('a');
    var tmp$_8;
    tmp$_8 = attrs_3.attributes.entries.iterator();
    while (tmp$_8.hasNext()) {
      var element_8 = tmp$_8.next();
      var name_3 = element_8.key;
      var value_3 = element_8.value;
      element_7.setAttribute(name_3, value_3.toString());
    }
    if (!attrs_3.classes.isEmpty() || attrs_3.klass != null) {
      element_7.setAttribute('class', joinToString(filterNotNull(plus_0(attrs_3.classes, attrs_3.klass)), ' '));
    }
    var t_3 = Kotlin.isType(tmp$_7 = element_7, Element_0) ? tmp$_7 : throwCCE();
    if (attrs_3.ref != null)
      attrs_3.ref.currentOrNull = t_3;
    boundClassByPredicate(t_3, Header_getInstance().activePage, [ACTIVE_PAGE_CLASS], void 0, header$lambda$lambda$lambda_0);
    text(t_3, 'Courses');
    on(t_3, 'click', routeLink$lambda$lambda(href));
    t.appendChild(element_7);
    var href_0 = '/calendar';
    var attrs_4 = (new CommonAttributes()).mergeWith_zb9t9x$(mapOf(to('href', href_0)));
    var tmp$_9;
    var element_9 = document.createElement('a');
    var tmp$_10;
    tmp$_10 = attrs_4.attributes.entries.iterator();
    while (tmp$_10.hasNext()) {
      var element_10 = tmp$_10.next();
      var name_4 = element_10.key;
      var value_4 = element_10.value;
      element_9.setAttribute(name_4, value_4.toString());
    }
    if (!attrs_4.classes.isEmpty() || attrs_4.klass != null) {
      element_9.setAttribute('class', joinToString(filterNotNull(plus_0(attrs_4.classes, attrs_4.klass)), ' '));
    }
    var t_4 = Kotlin.isType(tmp$_9 = element_9, Element_0) ? tmp$_9 : throwCCE();
    if (attrs_4.ref != null)
      attrs_4.ref.currentOrNull = t_4;
    boundClassByPredicate(t_4, Header_getInstance().activePage, [ACTIVE_PAGE_CLASS], void 0, header$lambda$lambda$lambda_1);
    text(t_4, 'Calendar');
    on(t_4, 'click', routeLink$lambda$lambda(href_0));
    t.appendChild(element_9);
    $receiver.appendChild(element);
  }
  function globalTheme$lambda$lambda($receiver) {
    set_fontFamily($receiver, "'Roboto', sans-serif");
    return Unit;
  }
  function globalTheme$lambda($receiver) {
    set_margin($receiver, get_px(0));
    set_padding($receiver, get_px(0));
    $receiver.invoke_pt2paz$(matchAny($receiver), globalTheme$lambda$lambda);
    return Unit;
  }
  var globalTheme;
  function rootContainer$lambda($receiver) {
    set_display($receiver, 'flex');
    set_flexDirection($receiver, 'column');
    set_height($receiver, get_vh(100));
    return Unit;
  }
  var rootContainer;
  function contentContainer$lambda($receiver) {
    set_backgroundColor($receiver, Theme_getInstance().background.toString());
    set_color($receiver, Theme_getInstance().onBackground.toString());
    set_flexGrow($receiver, '10');
    set_flexShrink($receiver, '1');
    set_flexBasis($receiver, 'auto');
    set_height($receiver, get_percent(100));
    return Unit;
  }
  var contentContainer;
  function main$lambda$lambda$lambda$lambda($receiver) {
    return Unit;
  }
  function main$lambda$lambda$lambda$lambda_0($receiver) {
    Header_getInstance().activePage.currentValue = Page$HOME_getInstance();
    text($receiver, 'Root');
    return Unit;
  }
  function main$lambda$lambda$lambda$lambda_1($receiver) {
    $receiver.unaryPlus_pdl1vz$('courses');
    return Unit;
  }
  function main$lambda$lambda$lambda$lambda_2($receiver) {
    courses($receiver);
    return Unit;
  }
  function main$lambda$lambda$lambda$lambda_3($receiver) {
    $receiver.unaryPlus_pdl1vz$('calendar');
    return Unit;
  }
  function main$lambda$lambda$lambda$lambda_4($receiver) {
    Header_getInstance().activePage.currentValue = Page$CALENDAR_getInstance();
    text($receiver, 'Calendar');
    for (var index = 0; index < 10; index++) {
      Toasts_getInstance().push_1c7o5j$(new Toast(ToastType$INFO_getInstance(), 'This is a test ' + index, L1000));
    }
    return Unit;
  }
  function main$lambda$lambda$lambda($receiver) {
    $receiver.route_18ee83$(main$lambda$lambda$lambda$lambda, main$lambda$lambda$lambda$lambda_0);
    $receiver.route_18ee83$(main$lambda$lambda$lambda$lambda_1, main$lambda$lambda$lambda$lambda_2);
    $receiver.route_18ee83$(main$lambda$lambda$lambda$lambda_3, main$lambda$lambda$lambda$lambda_4);
    return Unit;
  }
  function main() {
    rawCSS("@import url('https://fonts.googleapis.com/css?family=Roboto:400,500&display=swap');");
    var body = ensureNotNull(document.body);
    body.classList.add(reset);
    body.classList.add(globalTheme);
    var attrs = new CommonAttributes(rootContainer);
    var tmp$;
    var element = document.createElement('div');
    var tmp$_0;
    tmp$_0 = attrs.attributes.entries.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      var name = element_0.key;
      var value = element_0.value;
      element.setAttribute(name, value.toString());
    }
    if (!attrs.classes.isEmpty() || attrs.klass != null) {
      element.setAttribute('class', joinToString(filterNotNull(plus_0(attrs.classes, attrs.klass)), ' '));
    }
    var t = Kotlin.isType(tmp$ = element, Element_0) ? tmp$ : throwCCE();
    if (attrs.ref != null)
      attrs.ref.currentOrNull = t;
    toasts(t);
    header(t);
    var attrs_0 = new CommonAttributes(contentContainer);
    var tmp$_1;
    var element_1 = document.createElement('div');
    var tmp$_2;
    tmp$_2 = attrs_0.attributes.entries.iterator();
    while (tmp$_2.hasNext()) {
      var element_2 = tmp$_2.next();
      var name_0 = element_2.key;
      var value_0 = element_2.value;
      element_1.setAttribute(name_0, value_0.toString());
    }
    if (!attrs_0.classes.isEmpty() || attrs_0.klass != null) {
      element_1.setAttribute('class', joinToString(filterNotNull(plus_0(attrs_0.classes, attrs_0.klass)), ' '));
    }
    var t_0 = Kotlin.isType(tmp$_1 = element_1, Element_0) ? tmp$_1 : throwCCE();
    if (attrs_0.ref != null)
      attrs_0.ref.currentOrNull = t_0;
    router(t_0, main$lambda$lambda$lambda);
    t.appendChild(element_1);
    body.appendChild(element);
  }
  function RPCNamespace(namespace) {
    this.namespace = namespace;
  }
  RPCNamespace.prototype.rpc_26bn4z$ = function (name) {
    return new RPC(this.namespace, name);
  };
  RPCNamespace.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'RPCNamespace',
    interfaces: []
  };
  function RPC(namespace, name) {
    this.namespace = namespace;
    this.name = name;
  }
  RPC.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'RPC',
    interfaces: []
  };
  RPC.prototype.component1 = function () {
    return this.namespace;
  };
  RPC.prototype.component2 = function () {
    return this.name;
  };
  RPC.prototype.copy_puj7f4$ = function (namespace, name) {
    return new RPC(namespace === void 0 ? this.namespace : namespace, name === void 0 ? this.name : name);
  };
  RPC.prototype.toString = function () {
    return 'RPC(namespace=' + Kotlin.toString(this.namespace) + (', name=' + Kotlin.toString(this.name)) + ')';
  };
  RPC.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.namespace) | 0;
    result = result * 31 + Kotlin.hashCode(this.name) | 0;
    return result;
  };
  RPC.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.namespace, other.namespace) && Kotlin.equals(this.name, other.name)))));
  };
  function call$lambda$lambda(this$call, closure$resolve) {
    return function () {
      var tmp$;
      if (equals(this$call, CoursesBackend_getInstance().list)) {
        closure$resolve((tmp$ = listOf([new Course('Foo'), new Course('Bar')])) == null || Kotlin.isType(tmp$, Any) ? tmp$ : throwCCE());
      }
      return Unit;
    };
  }
  function call$lambda(this$call) {
    return function (resolve, reject) {
      window.setTimeout(call$lambda$lambda(this$call, resolve), 1000);
      return Unit;
    };
  }
  function call($receiver, request) {
    return new Promise(call$lambda($receiver));
  }
  function elevations$lambda$lambda(closure$elevation) {
    return function ($receiver) {
      set_backgroundColor($receiver, Theme_getInstance().surface.toString());
      set_color($receiver, Theme_getInstance().onSurface.toString());
      set_boxShadow($receiver, boxShadow_0(1, 1, closure$elevation + 5 | 0, 0, 'rgba(0, 0, 0, 0.25)'));
      set_padding($receiver, get_px(16));
      set_borderRadius($receiver, get_px(3));
      return Unit;
    };
  }
  var elevations;
  function surface($receiver, attrs, elevation, children) {
    if (attrs === void 0)
      attrs = new CommonAttributes();
    if (elevation === void 0)
      elevation = 1;
    var tmp$ = void 0;
    var tmp$_0 = attrs.classes;
    var tmp$_1 = elevations;
    var b = elevations.size;
    var b_0 = Math_0.min(elevation, b);
    var attrs_0 = attrs.copy_s0ghyx$(tmp$, plus_0(tmp$_0, tmp$_1.get_za3lpa$(Math_0.max(0, b_0))));
    var tmp$_2;
    var element = document.createElement('div');
    var tmp$_3;
    tmp$_3 = attrs_0.attributes.entries.iterator();
    while (tmp$_3.hasNext()) {
      var element_0 = tmp$_3.next();
      var name = element_0.key;
      var value = element_0.value;
      element.setAttribute(name, value.toString());
    }
    if (!attrs_0.classes.isEmpty() || attrs_0.klass != null) {
      element.setAttribute('class', joinToString(filterNotNull(plus_0(attrs_0.classes, attrs_0.klass)), ' '));
    }
    var t = Kotlin.isType(tmp$_2 = element, Element_0) ? tmp$_2 : throwCCE();
    if (attrs_0.ref != null)
      attrs_0.ref.currentOrNull = t;
    children(t);
    $receiver.appendChild(element);
  }
  function Theme() {
    Theme_instance = this;
    this.primary = new ColorShades(RGB$Companion_getInstance().create_za3lpa$(6765239));
    this.secondary = new ColorShades(RGB$Companion_getInstance().create_za3lpa$(240116));
    this.background = RGB$Companion_getInstance().create_za3lpa$(15066597);
    this.surface = RGB$Companion_getInstance().create_za3lpa$(16777215);
    this.error = RGB$Companion_getInstance().create_za3lpa$(11534368);
    this.onPrimary = RGB$Companion_getInstance().create_za3lpa$(16777215);
    this.onSecondary = RGB$Companion_getInstance().create_za3lpa$(16777215);
    this.onBackground = RGB$Companion_getInstance().create_za3lpa$(0);
    this.onSurface = RGB$Companion_getInstance().create_za3lpa$(0);
    this.onError = RGB$Companion_getInstance().create_za3lpa$(16777215);
  }
  Theme.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Theme',
    interfaces: []
  };
  var Theme_instance = null;
  function Theme_getInstance() {
    if (Theme_instance === null) {
      new Theme();
    }
    return Theme_instance;
  }
  _.BoundData = BoundData;
  _.boundText_lz5936$ = boundText;
  _.boundClass_6y1gue$ = boundClass;
  _.boundClassByPredicate_4ysx73$ = boundClassByPredicate;
  Object.defineProperty(_, 'CSS', {
    get: CSS_getInstance
  });
  $$importsForInline$$.web2 = _;
  _.rawCSS_61zpoe$ = rawCSS;
  _.globalCSS_yglqf4$ = globalCSS;
  _.globalCSS_mvjluj$ = globalCSS_0;
  _.css_j0tmw1$ = css;
  _.CSSBuilder = CSSBuilder;
  _.CSSPropertyListBuilder = CSSPropertyListBuilder;
  _.WriteOnlyProperty = WriteOnlyProperty;
  _.get_textDecoration_ohgl7a$ = get_textDecoration;
  _.set_textDecoration_ob3quk$ = set_textDecoration;
  _.get_color_ohgl7a$ = get_color;
  _.set_color_ob3quk$ = set_color;
  _.get_transition_ohgl7a$ = get_transition;
  _.set_transition_ob3quk$ = set_transition;
  _.get_position_ohgl7a$ = get_position;
  _.set_position_ob3quk$ = set_position;
  _.get_top_ohgl7a$ = get_top;
  _.set_top_ob3quk$ = set_top;
  _.get_bottom_ohgl7a$ = get_bottom;
  _.set_bottom_ob3quk$ = set_bottom;
  _.get_left_ohgl7a$ = get_left;
  _.set_left_ob3quk$ = set_left;
  _.get_right_ohgl7a$ = get_right;
  _.set_right_ob3quk$ = set_right;
  _.get_backgroundColor_ohgl7a$ = get_backgroundColor;
  _.set_backgroundColor_ob3quk$ = set_backgroundColor;
  _.get_content_ohgl7a$ = get_content;
  _.set_content_ob3quk$ = set_content;
  _.get_opacity_ohgl7a$ = get_opacity;
  _.set_opacity_ob3quk$ = set_opacity;
  _.get_outline_ohgl7a$ = get_outline;
  _.set_outline_ob3quk$ = set_outline;
  _.get_display_ohgl7a$ = get_display;
  _.set_display_ob3quk$ = set_display;
  _.get_padding_ohgl7a$ = get_padding;
  _.set_padding_ob3quk$ = set_padding;
  _.get_paddingTop_ohgl7a$ = get_paddingTop;
  _.set_paddingTop_ob3quk$ = set_paddingTop;
  _.get_paddingBottom_ohgl7a$ = get_paddingBottom;
  _.set_paddingBottom_ob3quk$ = set_paddingBottom;
  _.get_paddingLeft_ohgl7a$ = get_paddingLeft;
  _.set_paddingLeft_ob3quk$ = set_paddingLeft;
  _.get_paddingRight_ohgl7a$ = get_paddingRight;
  _.set_paddingRight_ob3quk$ = set_paddingRight;
  _.get_border_ohgl7a$ = get_border;
  _.set_border_ob3quk$ = set_border;
  _.get_borderRadius_ohgl7a$ = get_borderRadius;
  _.set_borderRadius_ob3quk$ = set_borderRadius;
  _.get_width_ohgl7a$ = get_width;
  _.set_width_ob3quk$ = set_width;
  _.get_height_ohgl7a$ = get_height;
  _.set_height_ob3quk$ = set_height;
  _.get_margin_ohgl7a$ = get_margin;
  _.set_margin_ob3quk$ = set_margin;
  _.get_marginTop_ohgl7a$ = get_marginTop;
  _.set_marginTop_ob3quk$ = set_marginTop;
  _.get_marginLeft_ohgl7a$ = get_marginLeft;
  _.set_marginLeft_ob3quk$ = set_marginLeft;
  _.get_marginRight_ohgl7a$ = get_marginRight;
  _.set_marginRight_ob3quk$ = set_marginRight;
  _.get_marginBottom_ohgl7a$ = get_marginBottom;
  _.set_marginBottom_ob3quk$ = set_marginBottom;
  _.get_alignItems_ohgl7a$ = get_alignItems;
  _.set_alignItems_ob3quk$ = set_alignItems;
  _.get_justifyContent_ohgl7a$ = get_justifyContent;
  _.set_justifyContent_ob3quk$ = set_justifyContent;
  _.get_justifyItems_ohgl7a$ = get_justifyItems;
  _.set_justifyItems_ob3quk$ = set_justifyItems;
  _.get_flexDirection_ohgl7a$ = get_flexDirection;
  _.set_flexDirection_ob3quk$ = set_flexDirection;
  _.get_flexFlow_ohgl7a$ = get_flexFlow;
  _.set_flexFlow_ob3quk$ = set_flexFlow;
  _.get_flexGrow_ohgl7a$ = get_flexGrow;
  _.set_flexGrow_ob3quk$ = set_flexGrow;
  _.get_flexShrink_ohgl7a$ = get_flexShrink;
  _.set_flexShrink_ob3quk$ = set_flexShrink;
  _.get_flexBasis_ohgl7a$ = get_flexBasis;
  _.set_flexBasis_ob3quk$ = set_flexBasis;
  _.get_boxSizing_ohgl7a$ = get_boxSizing;
  _.set_boxSizing_ob3quk$ = set_boxSizing;
  _.get_resize_ohgl7a$ = get_resize;
  _.set_resize_ob3quk$ = set_resize;
  _.get_fontSize_ohgl7a$ = get_fontSize;
  _.set_fontSize_ob3quk$ = set_fontSize;
  _.get_fontWeight_ohgl7a$ = get_fontWeight;
  _.set_fontWeight_ob3quk$ = set_fontWeight;
  _.get_fontFamily_ohgl7a$ = get_fontFamily;
  _.set_fontFamily_ob3quk$ = set_fontFamily;
  _.get_listStyle_ohgl7a$ = get_listStyle;
  _.set_listStyle_ob3quk$ = set_listStyle;
  _.get_maxWidth_ohgl7a$ = get_maxWidth;
  _.set_maxWidth_ob3quk$ = set_maxWidth;
  _.get_maxHeight_ohgl7a$ = get_maxHeight;
  _.set_maxHeight_ob3quk$ = set_maxHeight;
  _.get_minHeight_ohgl7a$ = get_minHeight;
  _.set_minHeight_ob3quk$ = set_minHeight;
  _.get_minWidth_ohgl7a$ = get_minWidth;
  _.set_minWidth_ob3quk$ = set_minWidth;
  _.get_borderCollapse_ohgl7a$ = get_borderCollapse;
  _.set_borderCollapse_ob3quk$ = set_borderCollapse;
  _.get_borderSpacing_ohgl7a$ = get_borderSpacing;
  _.set_borderSpacing_ob3quk$ = set_borderSpacing;
  _.get_textAlign_ohgl7a$ = get_textAlign;
  _.set_textAlign_ob3quk$ = set_textAlign;
  _.get_boxShadow_ohgl7a$ = get_boxShadow;
  _.set_boxShadow_ob3quk$ = set_boxShadow;
  _.CSSDelegate = CSSDelegate;
  _.isUpperCase_myv2d0$ = isUpperCase;
  _.isLowerCase_myv2d0$ = isLowerCase;
  _.CSSSelector = CSSSelector;
  _.CSSSelectorContext = CSSSelectorContext;
  _.byTag_oc52fw$ = byTag;
  _.byClass_oc52fw$ = byClass;
  _.byId_oc52fw$ = byId;
  _.byNamespace_oc52fw$ = byNamespace;
  _.matchAny_346v1a$ = matchAny;
  _.matchSelf_346v1a$ = matchSelf;
  _.withNoNamespace_346v1a$ = withNoNamespace;
  _.attributePresent_12iqa$ = attributePresent;
  _.attributeEquals_n98xkp$ = attributeEquals;
  _.attributeListContains_n98xkp$ = attributeListContains;
  _.attributeEqualsHyphen_n98xkp$ = attributeEqualsHyphen;
  _.attributeStartsWith_n98xkp$ = attributeStartsWith;
  _.attributeEndsWith_n98xkp$ = attributeEndsWith;
  _.attributeContains_n98xkp$ = attributeContains;
  _.withPseudoClass_i84tkf$ = withPseudoClass;
  _.withPseudoElement_i84tkf$ = withPseudoElement;
  _.adjacentSibling_asarsl$ = adjacentSibling;
  _.anySibling_asarsl$ = anySibling;
  _.directChild_asarsl$ = directChild;
  _.descendant_asarsl$ = descendant;
  _.or_asarsl$ = or;
  _.and_asarsl$ = and;
  _.get_pt_s8ev3n$ = get_pt;
  _.get_px_s8ev3n$ = get_px;
  _.get_vh_s8ev3n$ = get_vh;
  _.get_em_s8ev3n$ = get_em;
  _.get_percent_s8ev3n$ = get_percent;
  _.CSSVar = CSSVar;
  _.variable_aziiin$ = variable;
  _.setVariable_oulqtc$ = setVariable;
  _.setVariable_ppjwj3$ = setVariable_0;
  _.boxShadow_ovbccf$ = boxShadow_0;
  _.CardInStack = CardInStack;
  _.cardStack_fyhz0v$ = cardStack;
  Object.defineProperty(RGB, 'Companion', {
    get: RGB$Companion_getInstance
  });
  _.RGB = RGB;
  _.lighten_mr7pj8$ = lighten;
  _.darken_mr7pj8$ = darken;
  _.ColorShades = ColorShades;
  _.Reference = Reference;
  _.baseElement_q99izq$ = baseElement;
  _.text_46n0ku$ = text;
  _.on_z3ui9j$ = on;
  _.a_8tcszn$ = a;
  _.div_9dg6av$ = div;
  _.h1_m4p30b$ = h1;
  _.h2_m4p30b$ = h2;
  _.h3_m4p30b$ = h3;
  _.h4_m4p30b$ = h4;
  _.h5_m4p30b$ = h5;
  _.h6_m4p30b$ = h6;
  _.ul_ydw3hf$ = ul;
  _.li_nj1tw5$ = li;
  _.form_yx7vw5$ = form;
  _.input_x6u7o5$ = input;
  Object.defineProperty(WrapType, 'soft', {
    get: WrapType$soft_getInstance
  });
  Object.defineProperty(WrapType, 'hard', {
    get: WrapType$hard_getInstance
  });
  _.WrapType = WrapType;
  _.textarea_du7nhi$ = textarea;
  _.button_z7y0j9$ = button;
  _.CommonAttributes = CommonAttributes;
  _.onDeinit_a27nih$ = onDeinit;
  Object.defineProperty(_, 'RegisteredHooks', {
    get: RegisteredHooks_getInstance
  });
  _.deleteNode_2rdptt$ = deleteNode;
  Object.defineProperty(_, 'flexCss', {
    get: function () {
      return flexCss;
    }
  });
  Object.defineProperty(_, 'AlignItems', {
    get: AlignItems_getInstance
  });
  Object.defineProperty(_, 'JustifyItems', {
    get: JustifyItems_getInstance
  });
  Object.defineProperty(_, 'JustifyContent', {
    get: JustifyContent_getInstance
  });
  _.flex_9dg6av$ = flex;
  _.ListComponent = ListComponent;
  _.list_9jqv7i$ = list;
  _.LoadingState = LoadingState;
  _.loading_fsmwhl$ = loading;
  _.RemoteDataComponent = RemoteDataComponent;
  _.remoteDataWithLoading_lf84ih$ = remoteDataWithLoading;
  _.loadingIcon_ejp6nk$ = loadingIcon;
  Object.defineProperty(_, 'reset', {
    get: function () {
      return reset;
    }
  });
  _.routeLink_8tcszn$ = routeLink;
  Router.prototype.RouteWithGenerator = Router$RouteWithGenerator;
  Object.defineProperty(_, 'Router', {
    get: Router_getInstance
  });
  _.router_ok5kny$ = router;
  _.Route = Route;
  RouteSegment.Plain = RouteSegment$Plain;
  Object.defineProperty(RouteSegment, 'Remaining', {
    get: RouteSegment$Remaining_getInstance
  });
  Object.defineProperty(RouteSegment, 'Wildcard', {
    get: RouteSegment$Wildcard_getInstance
  });
  _.RouteSegment = RouteSegment;
  _.RouteBuilder = RouteBuilder;
  Object.defineProperty(ToastType, 'INFO', {
    get: ToastType$INFO_getInstance
  });
  _.ToastType = ToastType;
  _.Toast = Toast;
  Object.defineProperty(_, 'Toasts', {
    get: Toasts_getInstance
  });
  _.toasts_ejp6nk$ = toasts;
  var package$edu = _.edu || (_.edu = {});
  package$edu.Course = Course;
  Object.defineProperty(package$edu, 'CoursesBackend', {
    get: CoursesBackend_getInstance
  });
  package$edu.courses_ejp6nk$ = courses;
  Object.defineProperty(Page, 'HOME', {
    get: Page$HOME_getInstance
  });
  Object.defineProperty(Page, 'COURSES', {
    get: Page$COURSES_getInstance
  });
  Object.defineProperty(Page, 'CALENDAR', {
    get: Page$CALENDAR_getInstance
  });
  package$edu.Page = Page;
  Object.defineProperty(package$edu, 'Header', {
    get: Header_getInstance
  });
  package$edu.header_ejp6nk$ = header;
  package$edu.main = main;
  package$edu.RPCNamespace = RPCNamespace;
  package$edu.RPC = RPC;
  package$edu.call_umhkl4$ = call;
  package$edu.surface_2gihdx$ = surface;
  Object.defineProperty(package$edu, 'Theme', {
    get: Theme_getInstance
  });
  cssNamespaceId = 0;
  textDecoration = new CSSDelegate();
  color = new CSSDelegate();
  transition = new CSSDelegate();
  position = new CSSDelegate();
  top = new CSSDelegate();
  bottom = new CSSDelegate();
  left = new CSSDelegate();
  right = new CSSDelegate();
  backgroundColor = new CSSDelegate();
  content = new CSSDelegate();
  opacity = new CSSDelegate();
  outline = new CSSDelegate();
  display = new CSSDelegate();
  padding = new CSSDelegate();
  paddingTop = new CSSDelegate();
  paddingBottom = new CSSDelegate();
  paddingLeft = new CSSDelegate();
  paddingRight = new CSSDelegate();
  border = new CSSDelegate();
  borderRadius = new CSSDelegate();
  width = new CSSDelegate();
  height = new CSSDelegate();
  margin = new CSSDelegate();
  marginTop = new CSSDelegate();
  marginLeft = new CSSDelegate();
  marginRight = new CSSDelegate();
  marginBottom = new CSSDelegate();
  alignItems = new CSSDelegate();
  justifyContent = new CSSDelegate();
  justifyItems = new CSSDelegate();
  flexDirection = new CSSDelegate();
  flexFlow = new CSSDelegate();
  flexGrow = new CSSDelegate();
  flexShrink = new CSSDelegate();
  flexBasis = new CSSDelegate();
  boxSizing = new CSSDelegate();
  resize = new CSSDelegate();
  fontSize = new CSSDelegate();
  fontWeight = new CSSDelegate();
  fontFamily = new CSSDelegate();
  listStyle = new CSSDelegate();
  maxWidth = new CSSDelegate();
  maxHeight = new CSSDelegate();
  minHeight = new CSSDelegate();
  minWidth = new CSSDelegate();
  borderCollapse = new CSSDelegate();
  borderSpacing = new CSSDelegate();
  textAlign = new CSSDelegate();
  boxShadow = new CSSDelegate();
  SELF_SELECTOR = '##SELF##';
  flexCss = css(flexCss$lambda);
  loadingIconStyle = css(loadingIconStyle$lambda);
  reset = css(reset$lambda);
  TOAST_ACTIVE = 'active';
  toastStyle = css(toastStyle$lambda);
  container = css(container$lambda);
  coursesSurface = css(coursesSurface$lambda);
  ACTIVE_PAGE_CLASS = 'active';
  style = css(style$lambda);
  globalTheme = css(globalTheme$lambda);
  rootContainer = css(rootContainer$lambda);
  contentContainer = css(contentContainer$lambda);
  var $receiver = new IntRange(1, 10);
  var destination = ArrayList_init_0(collectionSizeOrDefault($receiver, 10));
  var tmp$;
  tmp$ = $receiver.iterator();
  while (tmp$.hasNext()) {
    var item = tmp$.next();
    destination.add_11rb$(css(elevations$lambda$lambda(item)));
  }
  elevations = destination;
  main();
  Kotlin.defineModule('web2', _);
  return _;
}));

//# sourceMappingURL=web2.js.map

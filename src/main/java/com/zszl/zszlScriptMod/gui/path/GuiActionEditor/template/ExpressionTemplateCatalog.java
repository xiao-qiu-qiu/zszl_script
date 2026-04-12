package com.zszl.zszlScriptMod.gui.path.GuiActionEditor.template;

import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ExpressionTemplateCard;

import java.util.ArrayList;
import java.util.List;

public final class ExpressionTemplateCatalog {
    private ExpressionTemplateCatalog() {
    }

    public static List<ExpressionTemplateCard> buildSetVarCards() {
        List<ExpressionTemplateCard> cards = new ArrayList<ExpressionTemplateCard>();
        cards.add(card("数字字面量", "123", "直接写整数或小数。", "123 / -4 / 3.14",
                "返回对应数字值", "number", "literal"));
        cards.add(card("字符串字面量", "\"boss\"", "用单引号或双引号包裹固定文本。", "\"text\" / 'text'",
                "\"boss\" -> boss", "string", "literal", "quoted"));
        cards.add(card("布尔字面量", "true", "直接写布尔值 true 或 false。", "true / false",
                "用于条件或开关值", "boolean", "literal"));
        cards.add(card("空值字面量", "null", "直接写 null，也支持 nil 别名。", "null / nil",
                "可配合 ?? 或 coalesce 使用", "null", "nil", "literal"));
        cards.add(card("括号分组", "(global.money + 1) * 2", "用括号强制指定计算顺序。", "(表达式)",
                "先加后乘", "group", "priority"));
        cards.add(card("当前变量 += 值", "+= 1", "用当前目标变量的旧值加上新值，适合累计计数。", "[+|-|*|/|%]= 表达式",
                "旧值 5 -> 结果 6", "shorthand", "plusEquals"));
        cards.add(card("完整变量 += 写法", "global.money += 5", "也可以对任意变量先取值再参与简写运算。", "变量 [+|-|*|/|%]= 表达式",
                "等价于 (global.money) + 5", "fullShorthand", "assignment"));
        cards.add(card("当前变量 -= 值", "-= 1", "用当前目标变量的旧值减去新值。", "-= 表达式",
                "旧值 5 -> 结果 4", "minusEquals"));
        cards.add(card("当前变量 *= 值", "*= 2", "用当前目标变量的旧值乘以新值。", "*= 表达式",
                "旧值 5 -> 结果 10", "timesEquals"));
        cards.add(card("当前变量 /= 值", "/= 2", "用当前目标变量的旧值除以新值。", "/= 表达式",
                "旧值 8 -> 结果 4", "divideEquals"));
        cards.add(card("当前变量 %= 值", "%= 10", "对当前目标变量做取模运算。", "%= 表达式",
                "旧值 23 -> 结果 3", "modEquals"));
        cards.add(card("直接变量引用", "global.money", "直接读取一个作用域变量的值。", "scope.name 或 sequenceName",
                "返回变量当前值", "variable", "scope"));
        cards.add(card("流程内置变量", "step_index", "可直接读取 sequence_name、step_index、action_index、gui_title 这些运行时内置值。",
                "sequence_name / step_index / action_index / gui_title", "当前步骤序号等内置信息",
                "builtin", "runtime"));
        cards.add(card("玩家内置变量", "player_health",
                "可直接读取 player_x/y/z、block_x/y/z、yaw、pitch、name、health、food 等玩家运行时值。",
                "player_health / player_x / player_block_x ...", "返回当前玩家状态",
                "player", "builtin", "runtime"));
        cards.add(card("$变量引用", "$target.name", "用 $ 前缀读取运行时变量，适合模板化引用。", "$变量名",
                "返回目标变量值", "dollar", "runtime"));
        cards.add(card("点路径访问", "$target.info.name", "从对象或 Map 里继续读取子字段。", "变量.字段.字段",
                "返回 name 子字段", "path", "member"));
        cards.add(card("索引访问", "$targets[-1]", "从列表里按索引读取，支持负索引。", "变量[索引]",
                "[-1] 返回最后一个元素", "index", "list"));
        cards.add(card("列表字面量", "[\"boss\", 1, true]", "直接在表达式里声明一个列表。", "[值1, 值2, 值3]",
                "返回一个列表对象", "array", "literal"));
        cards.add(card("算术计算", "global.money + 1", "支持 +、-、*、/、% 组合数值表达式。", "表达式 运算符 表达式",
                "5 + 1 -> 6", "math", "arithmetic"));
        cards.add(card("逻辑与 &&", "global.money > 10 && local.loop_index < 3", "两个条件都成立时返回 true。",
                "条件 && 条件", "true / false", "and"));
        cards.add(card("逻辑或 ||", "exists(global.money) || exists(temp.money)", "任一条件成立就返回 true。",
                "条件 || 条件", "true / false", "or"));
        cards.add(card("逻辑非 !", "!contains(sequence.targets, \"boss\")", "把布尔结果反转。", "!条件",
                "true -> false", "not"));
        cards.add(card("三元 ?:",
                "temp.current_match == \"boss\" ? 1 : 0",
                "条件成立返回前值，否则返回后值。", "条件 ? 成立值 : 失败值", "匹配 boss 时返回 1",
                "ternary", "ifElse"));
        cards.add(card("空值回退 ??", "global.money ?? 0", "左侧为空时回退到右侧默认值。", "值1 ?? 值2",
                "null -> 0", "coalesceOp", "nullish"));
        cards.add(card("contains",
                "contains(sequence.targets, \"boss\")",
                "检查字符串或列表里是否包含目标值。", "contains(源值, 目标值)",
                "包含时返回 true", "list", "string"));
        cards.add(card("containsIgnoreCase",
                "containsIgnoreCase(temp.current_match, \"boss\")",
                "忽略大小写检查包含关系。", "containsIgnoreCase(源值, 目标值)", "Boss 也会匹配 boss",
                "containsignorecase"));
        cards.add(card("startsWith / startsWithIgnoreCase",
                "startsWithIgnoreCase(temp.current_match, \"boss\")",
                "判断文本前缀。", "startsWith(text, prefix)", "Boss_01 -> true",
                "startswith", "startswithignorecase"));
        cards.add(card("endsWith / endsWithIgnoreCase",
                "endsWith(temp.current_match, \"_done\")",
                "判断文本后缀。", "endsWith(text, suffix)", "task_done -> true",
                "endswith", "endswithignorecase"));
        cards.add(card("equalsIgnoreCase",
                "equalsIgnoreCase(temp.current_match, \"boss\")",
                "忽略大小写做全文相等比较。", "equalsIgnoreCase(text1, text2)", "Boss == boss -> true",
                "equalsignorecase"));
        cards.add(card("regex / matches",
                "regex(temp.current_match, \"boss_[0-9]+\")",
                "用正则匹配文本。", "regex(text, pattern)", "boss_12 -> true",
                "matches", "pattern"));
        cards.add(card("trim", "trim(temp.current_match)", "去掉文本首尾空白。", "trim(text)",
                "\" boss \" -> \"boss\"", "string"));
        cards.add(card("lower", "lower(temp.current_match)", "转成小写。", "lower(text)",
                "\"Boss\" -> \"boss\"", "lowercase"));
        cards.add(card("upper", "upper(temp.current_match)", "转成大写。", "upper(text)",
                "\"Boss\" -> \"BOSS\"", "uppercase"));
        cards.add(card("replace",
                "replace(temp.current_match, \"boss\", \"elite\")",
                "替换文本里的内容。", "replace(text, old, new)", "\"boss_1\" -> \"elite_1\"",
                "string"));
        cards.add(card("substring / substr",
                "substring(temp.current_match, 0, 4)",
                "截取文本片段。", "substring(text, start, end?)", "\"boss_1\" -> \"boss\"",
                "substr"));
        cards.add(card("indexOf / lastIndexOf",
                "indexOf(temp.current_match, \"_\")",
                "返回文本里第一次或最后一次出现的位置。", "indexOf(text, keyword)", "\"boss_1\" -> 4",
                "lastindexof"));
        cards.add(card("split",
                "split(temp.current_match, \"_\")",
                "按分隔符拆成列表。", "split(text, sep)", "\"boss_1\" -> [boss, 1]",
                "list"));
        cards.add(card("join",
                "join(sequence.targets, \",\")",
                "把列表按分隔符拼回文本。", "join(list, sep)", "[a,b] -> \"a,b\"",
                "string"));
        cards.add(card("coalesce",
                "coalesce(temp.money, global.money, 0)",
                "返回第一个非空值。", "coalesce(a, b, c...)", "null, 12, 0 -> 12",
                "fallback"));
        cards.add(card("if 函数",
                "if(global.money > 10, \"rich\", \"poor\")",
                "函数形式的条件选择。", "if(条件, 成立值, 失败值)", "money=12 -> rich",
                "ternary"));
        cards.add(card("empty / exists",
                "empty(temp.current_match)",
                "判断值是否为空，exists 则相反。", "empty(value) / exists(value)", "\"\" -> true",
                "null", "exists"));
        cards.add(card("len / size / count",
                "len(sequence.targets)",
                "求字符串、列表、集合长度。", "len(value)", "[a,b,c] -> 3",
                "size", "count"));
        cards.add(card("first / last",
                "last(sequence.targets)",
                "取列表第一个或最后一个元素。", "first(list) / last(list)", "[a,b,c] -> c",
                "list"));
        cards.add(card("any / all",
                "all(global.money > 0, local.loop_index < 5)",
                "any 任一成立，all 全部成立。", "any(a,b,...) / all(a,b,...)", "all(true,true) -> true",
                "bool"));
        cards.add(card("eq / ne",
                "eq(global.money, 100)",
                "函数形式的相等 / 不等判断。", "eq(a, b) / ne(a, b)", "100 == 100 -> true",
                "equal", "notEqual"));
        cards.add(card("gt / lt / gte / lte",
                "gte(global.money, 100)",
                "函数形式的大小比较。", "gt(a,b) / lt(a,b) / gte(a,b) / lte(a,b)", "money=120 -> true",
                "compare"));
        cards.add(card("min / max",
                "max(global.money, local.reward, 0)",
                "返回多个值中的最小或最大值。", "min(a,b,...) / max(a,b,...)", "max(2,9,4) -> 9",
                "math"));
        cards.add(card("sum",
                "sum(sequence.damage_list)",
                "对数字列表求和。", "sum(list 或 多个参数)", "[2,3,4] -> 9",
                "math"));
        cards.add(card("avg / average",
                "avg(sequence.damage_list)",
                "对数字列表求平均值。", "avg(list) / average(list)", "[2,4,6] -> 4",
                "mean"));
        cards.add(card("abs",
                "abs(local.delta)",
                "取绝对值。", "abs(number)", "-3 -> 3",
                "math"));
        cards.add(card("pow",
                "pow(local.level, 2)",
                "做乘方运算。", "pow(base, exponent)", "pow(3,2) -> 9",
                "math"));
        cards.add(card("clamp",
                "clamp(global.money, 0, 9999)",
                "把数值限制在区间内。", "clamp(value, min, max)", "12000 -> 9999",
                "range"));
        cards.add(card("between / betweenInc",
                "betweenInclusive(global.money, 10, 20)",
                "判断数值是否落在区间中。", "between(value,min,max) / betweenInclusive(value,min,max)", "15 -> true",
                "betweeninc", "betweeninclusive"));
        cards.add(card("toNumber / number / int",
                "toNumber(temp.current_match)",
                "把文本转成数字。", "toNumber(value)", "\"12\" -> 12",
                "number", "int", "toint"));
        cards.add(card("toBoolean / bool / boolean",
                "toBoolean(temp.current_match)",
                "把文本或数字转成布尔值。", "toBoolean(value)", "\"true\" -> true",
                "bool", "boolean"));
        cards.add(card("toString / string",
                "toString(global.money)",
                "把值转成字符串。", "toString(value)", "12 -> \"12\"",
                "string"));
        cards.add(card("round / floor / ceil",
                "round(global.money / 3)",
                "常用取整函数。", "round(x) / floor(x) / ceil(x)", "2.6 -> 3",
                "math"));
        return cards;
    }

    public static List<ExpressionTemplateCard> buildBooleanCards() {
        List<ExpressionTemplateCard> cards = new ArrayList<ExpressionTemplateCard>();
        cards.add(card("布尔字面量", "true", "直接返回 true 或 false。", "true / false",
                "true / false", "boolean", "literal"));
        cards.add(card("括号分组",
                "(global.money > 10 || local.loop_index == 0) && exists(temp.current_match)",
                "把多个布尔判断分组后再继续组合。", "(布尔表达式)", "true / false", "group", "priority"));
        cards.add(card("逻辑与 &&",
                "global.money > 10 && local.loop_index < 3",
                "左右两侧都成立时返回 true。", "条件 && 条件", "true / false", "and"));
        cards.add(card("逻辑或 ||",
                "exists(global.money) || exists(temp.money)",
                "任意一侧成立就返回 true。", "条件 || 条件", "true / false", "or"));
        cards.add(card("逻辑非 !",
                "!contains(sequence.targets, \"boss\")",
                "对布尔结果取反。", "!条件", "true / false", "not"));
        cards.add(card("相等比较 ==",
                "temp.current_match == \"boss\"",
                "比较两个值是否相等。", "值1 == 值2", "true / false", "equals"));
        cards.add(card("不等比较 !=",
                "global.money != 0",
                "比较两个值是否不相等。", "值1 != 值2", "true / false", "notEquals"));
        cards.add(card("大于比较 >",
                "global.money > 100",
                "左值大于右值时返回 true。", "值1 > 值2", "true / false", "greaterThan"));
        cards.add(card("大于等于比较 >=",
                "local.loop_index >= 1",
                "左值大于等于右值时返回 true。", "值1 >= 值2", "true / false", "greaterThanOrEqual"));
        cards.add(card("小于比较 <",
                "local.loop_index < 5",
                "左值小于右值时返回 true。", "值1 < 值2", "true / false", "lessThan"));
        cards.add(card("小于等于比较 <=",
                "player_health <= 10",
                "左值小于等于右值时返回 true。", "值1 <= 值2", "true / false", "lessThanOrEqual"));
        cards.add(card("区间组合判断",
                "$local.loop_index >= 1 && $local.loop_index <= 3",
                "用比较运算符组合出闭区间/开区间判断。", "a >= min && a <= max", "true / false",
                "range", "between"));
        cards.add(card("contains",
                "contains(sequence.targets, \"boss\")",
                "检查字符串或列表中是否包含目标值。", "contains(源值, 目标值)", "包含时返回 true",
                "contains", "list", "string"));
        cards.add(card("containsIgnoreCase",
                "containsIgnoreCase(temp.current_match, \"boss\")",
                "忽略大小写检查包含关系。", "containsIgnoreCase(源值, 目标值)", "Boss -> true",
                "containsignorecase"));
        cards.add(card("startsWith / startsWithIgnoreCase",
                "startsWithIgnoreCase(temp.current_match, \"boss\")",
                "判断文本前缀是否匹配。", "startsWith(text, prefix)", "Boss_01 -> true",
                "startswith", "startswithignorecase"));
        cards.add(card("endsWith / endsWithIgnoreCase",
                "endsWith(temp.current_match, \"_done\")",
                "判断文本后缀是否匹配。", "endsWith(text, suffix)", "task_done -> true",
                "endswith", "endswithignorecase"));
        cards.add(card("equalsIgnoreCase",
                "equalsIgnoreCase(temp.current_match, \"boss\")",
                "忽略大小写做全文相等比较。", "equalsIgnoreCase(text1, text2)", "Boss == boss -> true",
                "equalsignorecase"));
        cards.add(card("regex / matches",
                "regex(temp.current_match, \"boss_[0-9]+\")",
                "用正则匹配文本。", "regex(text, pattern)", "boss_12 -> true",
                "matches", "pattern"));
        cards.add(card("empty / exists",
                "exists(global.money)",
                "判断值是否存在，empty 则判断是否为空。", "empty(value) / exists(value)",
                "exists(12) -> true", "empty", "exists"));
        cards.add(card("any / all",
                "all(global.money > 0, local.loop_index < 5)",
                "any 任一成立，all 全部成立。", "any(a,b,...) / all(a,b,...)", "all(true,true) -> true",
                "any", "all"));
        cards.add(card("eq / ne",
                "eq(global.money, 100)",
                "函数形式的相等 / 不等判断。", "eq(a,b) / ne(a,b)", "eq(100,100) -> true",
                "eq", "ne"));
        cards.add(card("gt / lt / gte / lte",
                "gte(global.money, 100)",
                "函数形式的大小比较。", "gt(a,b) / lt(a,b) / gte(a,b) / lte(a,b)",
                "gte(120,100) -> true", "gt", "lt", "gte", "lte"));
        cards.add(card("between / betweenInclusive",
                "betweenInclusive(global.money, 10, 20)",
                "判断数值是否落在区间中。", "between(value,min,max) / betweenInclusive(value,min,max)",
                "15 -> true", "between", "betweeninc", "betweeninclusive"));
        cards.add(card("toBoolean / bool / boolean",
                "toBoolean(temp.current_match)",
                "把文本、数字或对象按运行时规则转成布尔值。", "toBoolean(value)", "\"true\" -> true",
                "toboolean", "bool", "boolean"));
        return cards;
    }

    private static ExpressionTemplateCard card(String name, String example, String description, String format,
            String outputExample, String... keywords) {
        return new ExpressionTemplateCard(name, example, description, format, outputExample, keywords);
    }
}

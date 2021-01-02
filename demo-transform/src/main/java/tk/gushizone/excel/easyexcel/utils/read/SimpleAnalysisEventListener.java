package tk.gushizone.excel.easyexcel.utils.read;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import tk.gushizone.excel.easyexcel.utils.EasyExcelUtils;
import tk.gushizone.excel.easyexcel.utils.common.ExcelHeadColumn;
import tk.gushizone.excel.easyexcel.utils.exception.ExcelException;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author gushizone@gmail.com
 * @date 2021-01-02 15:06
 */
@Slf4j
@Getter
public class SimpleAnalysisEventListener<T> extends AnalysisEventListener<T> {

    /**
     * 表头数据
     */
    private List<ExcelHeadColumn> headColumns;

    /**
     * 数据结果集
     */
    private final List<T> results;

    public SimpleAnalysisEventListener() {
        results = Lists.newArrayListWithExpectedSize(2000);
    }

    /**
     * 每行表头解析完触发
     */
    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        if (context.readRowHolder().getRowIndex() != context.readSheetHolder().getHeadRowNumber() - 1) {
            return;
        }

        // 读取表头
        headColumns = EasyExcelUtils.readHeadColumn(context.currentReadHolder().excelReadHeadProperty().getHeadClazz());
        // 校验模板并设置索引位
        Map<String, ExcelHeadColumn> nameMap = headColumns.stream().collect(Collectors.toMap(ExcelHeadColumn::getName, Function.identity(), (a, b) -> a));
        for (Map.Entry<Integer, String> headEntry : headMap.entrySet()) {
            ExcelHeadColumn excelHeadColumn = nameMap.get(headEntry.getValue());
            if (excelHeadColumn == null) {
                throw new ExcelException("校验失败，请下载最新的模板");
            }
            excelHeadColumn.setIndex(headEntry.getKey());
        }
    }

    /**
     * 每行数据解析完触发
     */
    @Override
    public void invoke(T data, AnalysisContext context) {

        try {
            Class<?> clazz = data.getClass();
            Integer rowIndex = context.readRowHolder().getRowIndex() + 1;
            for (ExcelHeadColumn headCol : headColumns) {
                if (!headCol.getExcelPropertyX()) {
                    continue;
                }

                PropertyDescriptor pd = new PropertyDescriptor(headCol.getFieldName(), clazz);
                Method readMethod = pd.getReadMethod();

                Object value = readMethod.invoke(data);

                // 必填校验
                if (headCol.getRequired()
                        && isEmpty(value)) {
                    throw new ExcelException("第{0}行, {1}不能为空", rowIndex, headCol.getName());
                }
                // 选项校验
                if (CollectionUtils.isNotEmpty(headCol.getOptions())
                        && !isEmpty(value)) {
                    String fieldValue = String.valueOf(value);
                    if (!headCol.getMultiChoice()
                            && !headCol.getOptions().contains(fieldValue)) {
                        throw new ExcelException("第{0}行, {1}填写不合法，请使用{2}", rowIndex, headCol.getName(), headCol.getOptions());
                    } else {
                        String[] fieldValues = StringUtils.split(fieldValue, EasyExcelUtils.MULTI_CHOICE_SEPARATOR);
                        if (Arrays.stream(fieldValues).anyMatch(e -> !headCol.getOptions().contains(e))) {
                            throw new ExcelException("第{0}行, {1}填写不合法，请使用{2}，并用[，]分隔", rowIndex, headCol.getName(), headCol.getOptions());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ExcelException(e.getMessage());
        }
        results.add(data);
    }

    /**
     * 全部解析完成后触发
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        if (context.readRowHolder().getRowIndex() < context.readSheetHolder().getHeadRowNumber() - 1) {
            throw new ExcelException("校验失败，请下载最新的模板");
        }
        log.info("数据解析完成，共计{}条。", results.size());
    }

    /**
     * 所有异常都会触发
     */
    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {

        if (exception instanceof ExcelException) {
            throw exception;
        }
        if (exception instanceof ExcelDataConvertException) {

            ExcelDataConvertException excelDataConvertException = (ExcelDataConvertException) exception;

            Map<Integer, ExcelHeadColumn> headColMap = headColumns.stream().collect(Collectors.toMap(ExcelHeadColumn::getIndex, e -> e));
            ExcelHeadColumn excelHeadColumn = headColMap.get(excelDataConvertException.getColumnIndex());

            log.error("第{}行，{}解析异常：[{}]", excelDataConvertException.getRowIndex() + 1, excelHeadColumn.getName(), excelDataConvertException.getCellData());
            throw new ExcelException("第{0}行，{1}解析异常：[{2}]", excelDataConvertException.getRowIndex() + 1, excelHeadColumn.getName(), excelDataConvertException.getCellData());
        }
    }

    private boolean isEmpty(final Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence) {
            return StringUtils.isBlank((CharSequence) value);
        }
        return false;
    }
}
package com.kellen.bean;

import com.baomidou.mybatisplus.extension.ddl.IDdl;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.function.Consumer;

/**
 * MyBatis-Plus 自动维护 PostgreSQL DDL。
 * <p>
 * report 当前不携带业务表，仅默认维护公共数据库基础表；后续新增报告业务表时再追加独立SQL脚本。
 *
 * @author sunkailun
 * @className PostgresqlDdl
 */
@Component
public class PostgresqlDdl implements IDdl {

    /**
     * 当前项目数据源。
     */
    private final DataSource dataSource;

    /**
     * 构造MyBatis-Plus自动维护DDL组件。
     *
     * @param dataSource 当前项目数据源
     * @return void
     * @author sunkailun
     */
    public PostgresqlDdl(DataSource dataSource) {
        this.dataSource = dataSource; // 保存当前应用数据源，交给MyBatis-Plus DDL运行器执行脚本。
    }

    /**
     * 指定执行脚本的数据源。
     *
     * @param consumer MyBatis-Plus DDL脚本执行器
     * @return void
     * @author sunkailun
     */
    @Override
    public void runScript(Consumer<DataSource> consumer) {
        consumer.accept(dataSource); // 使用当前应用数据源执行DDL脚本，避免业务代码手写建表SQL。
    }

    /**
     * 获取自动维护DDL脚本列表。
     *
     * @return java.util.List<java.lang.String>
     * @author sunkailun
     */
    @Override
    public List<String> getSqlFiles() {
        return List.of(
                "db/postgresql/common-infra-schema.sql"
        );
    }
}

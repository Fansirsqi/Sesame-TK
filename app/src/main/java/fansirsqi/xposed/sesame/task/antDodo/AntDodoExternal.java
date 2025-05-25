package fansirsqi.xposed.sesame.task.antDodo;

import java.util.List;

import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;

public final class AntDodoExternal implements SelectModelField.SelectListFunc {
    @Override
    public final List<AlipayUser> getList() {
        return AlipayUser.getList();
    }
}

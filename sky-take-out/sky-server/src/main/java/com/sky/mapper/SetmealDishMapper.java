package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品ids查询对应的套餐ids
     * @param dishIds
     * @return
     */
    // select distinct setmeal_id from setmeal_dish where dish_id in (1,2,3);
    // 这玩意应该写在xml文件里去
    public List<Long> getSetmealIdsByDishIds(List<Long> dishIds);
}

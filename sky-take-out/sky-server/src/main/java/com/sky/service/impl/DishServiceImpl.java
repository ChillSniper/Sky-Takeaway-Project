package com.sky.service.impl;

import com.fasterxml.jackson.databind.util.BeanUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();

        BeanUtils.copyProperties(dishDTO, dish);

        // 1. 向菜品表插入1条数据
        dishMapper.insert(dish);

        Long dishId = dish.getId(); // 获取菜品id

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && !flavors.isEmpty()) {
            // 向口味表中插入n条数据

            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });

            dishFlavorMapper.insertBatch(flavors);

        }

        // 2. 向口味表插入n条数据
    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品批量删除
     *
     * @param ids
     */
    // 这个是事务注解，用于保证数据的一致性
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 先判断当前菜品是否能删除
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus() == StatusConstant.ENABLE) {
                // 起售中
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        // 菜品是否关联某个套餐 - 也就是说决定这个菜品是否能被删除
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds != null && !setmealIds.isEmpty()) {
            // 说明当前菜品关联了某个套餐，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 删除菜品表中的数据
        // 这边可以优化为批量删除

//        for (Long id : ids) {
//            dishMapper.deleteById();
//            // 删除菜品关联的口味数据
//            dishFlavorMapper.deleteByDishId(id);
//        }

        // 根据菜品id集合批量删除菜品数据和对应的口味数据
        dishMapper.deleteByIds(ids);

        dishFlavorMapper.deleteByDishIds(ids);

    }

    /**
     * 根据id查询菜品和对应的口味信息
     *
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id) {
        // 根据id查询菜品数据
        Dish dish = dishMapper.getById(id);
        // id->口味数据
        List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);
        // 将查询所得数据封装到VO
        if(dish != null) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);
            dishVO.setFlavors(flavors);
            return dishVO;
        }
        log.info("结果为空！！！");
        return null;
    }

    /**
     * 修改菜品和对应的口味信息
     * @param dishDTO
     */
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        // 修改基本信息
        dishMapper.update(dish);
        // 先把原有的口味信息全部删掉

        Long dishId = dish.getId();
        dishFlavorMapper.deleteByDishId(dishId);

        // 再把新的口味信息加入

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && !flavors.isEmpty()) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            // 向口味表中插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }

    }
}

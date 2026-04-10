#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
CPS 完整测试数据执行脚本
连接：127.0.0.1:3306  库：cps  用户：root  密码：fastbee

用法：python script/test/init_cps_test_data.py
"""
import os
import sys
import pymysql

# SQL 文件相对项目根目录的路径
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, '..', '..'))
CPS_ALL_IN_ONE_SQL = os.path.join(PROJECT_ROOT, 'backend', 'sql', 'module', 'cps-all-in-one.sql')

DB_CONFIG = dict(
    host='127.0.0.1', port=3306,
    user='root', password='fastbee',
    database='cps', charset='utf8mb4',
    autocommit=False
)


def get_conn():
    return pymysql.connect(**DB_CONFIG)


def check_tables(conn):
    cursor = conn.cursor()
    cursor.execute("SHOW TABLES LIKE 'cps_%'")
    tables = [r[0] for r in cursor.fetchall()]
    cursor.close()
    print(f'\n当前 CPS 表数量: {len(tables)}')
    for t in sorted(tables):
        cursor = conn.cursor()
        cursor.execute(f'SELECT COUNT(*) FROM `{t}`')
        cnt = cursor.fetchone()[0]
        cursor.close()
        print(f'  {t}: {cnt} 行')


# ──────────────────────────────────────────────
# STEP 1: 执行 cps-all-in-one.sql 建表
# ──────────────────────────────────────────────
def step1_schema(conn):
    print('\n========== STEP 1: 执行建表脚本 ==========')
    with open(CPS_ALL_IN_ONE_SQL, 'r', encoding='utf-8') as f:
        content = f.read()

    # 按分号分割，跳过注释行和空语句
    statements = []
    current = []
    for line in content.splitlines():
        stripped = line.strip()
        if stripped.startswith('--') or stripped == '':
            continue
        current.append(line)
        if stripped.endswith(';'):
            stmt = '\n'.join(current).strip().rstrip(';')
            if stmt:
                statements.append(stmt)
            current = []

    cursor = conn.cursor()
    success = 0
    skip = 0
    error = 0
    for stmt in statements:
        try:
            cursor.execute(stmt)
            success += 1
        except pymysql.err.OperationalError as e:
            code = e.args[0]
            # 1060=Duplicate column, 1061=Duplicate key, 1091=Can't DROP, 1050=Table already exists
            if code in (1060, 1061, 1091, 1050):
                skip += 1
            else:
                print(f'  WARN [{code}]: {str(e.args[1])[:80]}')
                print(f'       SQL: {stmt[:60]}...')
                error += 1
        except Exception as e:
            print(f'  ERROR: {e}')
            print(f'       SQL: {stmt[:80]}')
            error += 1
    cursor.close()
    conn.commit()
    print(f'  建表完成: 成功={success}, 跳过={skip}, 错误={error}')


# ──────────────────────────────────────────────
# STEP 2: 平台配置 + 供应商配置测试数据
# ──────────────────────────────────────────────
def step2_platform_vendor(conn):
    print('\n========== STEP 2: 平台 & 供应商测试数据 ==========')
    cursor = conn.cursor()

    cursor.execute("DELETE FROM cps_platform WHERE deleted=0")
    cursor.execute("DELETE FROM cps_api_vendor WHERE deleted=0")

    # cps_platform - 四大平台
    platforms = [
        ('taobao', '淘宝',   'DTK_TB_APP_KEY',  'DTK_TB_APP_SECRET',  'https://openapi.dataoke.com/api', 'mm_taobao_default_pid', 'dataoke', 'dataoke,haodan', 1),
        ('jd',     '京东',   'DTK_JD_APP_KEY',  'DTK_JD_APP_SECRET',  'https://openapi.dataoke.com/api', 'jd_default_unionid',    'dataoke', 'dataoke,haodan', 2),
        ('pdd',    '拼多多', 'DTK_PDD_APP_KEY', 'DTK_PDD_APP_SECRET', 'https://openapi.dataoke.com/api', 'pdd_default_pid',       'dataoke', 'dataoke',        3),
        ('douyin', '抖音',   'DY_APP_KEY',      'DY_APP_SECRET',      'https://openapi.jinritemai.com',  'dy_default_pid',        'official','official',        4),
    ]
    for p in platforms:
        cursor.execute("""
            INSERT INTO cps_platform
              (platform_code, platform_name, app_key, app_secret, api_base_url,
               default_adzone_id, active_vendor_code, supported_vendors,
               sort, status, tenant_id, creator)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,1,1,'admin')
        """, p)
    print(f'  ✓ cps_platform 插入 {len(platforms)} 条')

    # cps_api_vendor - 大淘客(淘宝+京东+拼多多) + 好单库(淘宝+京东)
    vendors = [
        ('dataoke', '大淘客', 'aggregator', 'taobao', 'DTK_TB_APP_KEY',  'DTK_TB_SECRET',  'https://openapi.dataoke.com/api', None,              'mm_dtk_taobao_pid', 10),
        ('dataoke', '大淘客', 'aggregator', 'jd',     'DTK_JD_APP_KEY',  'DTK_JD_SECRET',  'https://openapi.dataoke.com/api', 'DTK_JD_AUTH_TOKEN', None,              10),
        ('dataoke', '大淘客', 'aggregator', 'pdd',    'DTK_PDD_APP_KEY', 'DTK_PDD_SECRET', 'https://openapi.dataoke.com/api', None,              'pdd_dtk_pid',       10),
        ('haodan',  '好单库', 'aggregator', 'taobao', 'HDK_TB_APP_KEY',  'HDK_TB_SECRET',  'https://v2.api.haodanku.com',     None,              'mm_hdk_taobao_pid',  8),
        ('haodan',  '好单库', 'aggregator', 'jd',     'HDK_JD_APP_KEY',  'HDK_JD_SECRET',  'https://v2.api.haodanku.com',     None,              None,                 8),
    ]
    for v in vendors:
        cursor.execute("""
            INSERT INTO cps_api_vendor
              (vendor_code, vendor_name, vendor_type, platform_code,
               app_key, app_secret, api_base_url, auth_token, default_adzone_id,
               priority, status, tenant_id, creator)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,1,1,'admin')
        """, v)
    print(f'  ✓ cps_api_vendor 插入 {len(vendors)} 条')

    conn.commit()
    cursor.close()


# ──────────────────────────────────────────────
# STEP 3: 推广位(adzone)测试数据
# ──────────────────────────────────────────────
def step3_adzone(conn):
    print('\n========== STEP 3: 推广位测试数据 ==========')
    cursor = conn.cursor()
    cursor.execute("DELETE FROM cps_adzone WHERE deleted=0")

    adzones = [
        ('taobao', 'mm_taobao_default_pid', '淘宝默认推广位',     'general', None,      None, 1),
        ('taobao', 'mm_taobao_channel_pid', '淘宝渠道推广位A',    'channel', 'channel', 1001, 0),
        ('taobao', 'mm_taobao_member_1001', '会员1001专属推广位', 'member',  'member',  1001, 0),
        ('taobao', 'mm_taobao_member_1002', '会员1002专属推广位', 'member',  'member',  1002, 0),
        ('jd',     'jd_default_unionid',    '京东默认推广位',     'general', None,      None, 1),
        ('jd',     'jd_channel_pid_001',    '京东渠道推广位A',    'channel', 'channel', 1001, 0),
        ('pdd',    'pdd_default_pid',       '拼多多默认推广位',   'general', None,      None, 1),
        ('douyin', 'dy_default_pid',        '抖音默认推广位',     'general', None,      None, 1),
    ]
    for az in adzones:
        cursor.execute("""
            INSERT INTO cps_adzone
              (platform_code, adzone_id, adzone_name, adzone_type,
               relation_type, relation_id, is_default, status, tenant_id, creator)
            VALUES (%s,%s,%s,%s,%s,%s,%s,1,1,'admin')
        """, az)
    print(f'  ✓ cps_adzone 插入 {len(adzones)} 条')
    conn.commit()
    cursor.close()


# ──────────────────────────────────────────────
# STEP 4: 返利配置 + 冻结配置
# ──────────────────────────────────────────────
def step4_rebate_freeze_config(conn):
    print('\n========== STEP 4: 返利配置 & 冻结配置 ==========')
    cursor = conn.cursor()
    cursor.execute("DELETE FROM cps_rebate_config WHERE deleted=0")

    rebate_configs = [
        # (member_level_id, platform_code, rebate_rate, max_rebate_amount, min_rebate_amount, priority, status)
        # 全局兜底：全平台 80% 返佣
        (None, None,      80.0000, 0.00, 0.00,  1, 1),
        # 平台默认
        (None, 'taobao',  85.0000, 0.00, 0.00,  5, 1),
        (None, 'jd',      82.0000, 0.00, 0.00,  5, 1),
        (None, 'pdd',     80.0000, 0.00, 0.00,  5, 1),
        (None, 'douyin',  78.0000, 0.00, 0.00,  5, 1),
        # VIP 等级(level_id=2)
        (2,    'taobao',  90.0000, 500.00, 0.00, 10, 1),
        (2,    'jd',      88.0000, 500.00, 0.00, 10, 1),
        # 超级VIP(level_id=3)：全平台 92%
        (3,    None,      92.0000, 1000.00, 0.00, 15, 1),
    ]
    for rc in rebate_configs:
        cursor.execute("""
            INSERT INTO cps_rebate_config
              (member_level_id, platform_code, rebate_rate,
               max_rebate_amount, min_rebate_amount, priority, status, tenant_id, creator)
            VALUES (%s,%s,%s,%s,%s,%s,%s,1,'admin')
        """, rc)
    print(f'  ✓ cps_rebate_config 插入 {len(rebate_configs)} 条')

    # 冻结配置（幂等插入）
    cursor.execute("SELECT COUNT(*) FROM cps_freeze_config WHERE deleted=0")
    cnt = cursor.fetchone()[0]
    if cnt == 0:
        freeze_configs = [
            (None,      15, '全局默认配置-确认收货后15天解冻'),
            ('taobao',  15, '淘宝-确认收货后15天解冻'),
            ('jd',       7, '京东-确认收货后7天解冻'),
            ('pdd',     20, '拼多多-确认收货后20天解冻'),
        ]
        for fc in freeze_configs:
            cursor.execute("""
                INSERT INTO cps_freeze_config
                  (platform_code, unfreeze_days, status, remark, tenant_id, creator)
                VALUES (%s,%s,1,%s,1,'admin')
            """, fc)
        print(f'  ✓ cps_freeze_config 插入 {len(freeze_configs)} 条')
    else:
        for platform_code, days, remark in [
            ('taobao', 15, '淘宝-确认收货后15天解冻'),
            ('jd',      7, '京东-确认收货后7天解冻'),
            ('pdd',    20, '拼多多-确认收货后20天解冻'),
        ]:
            cursor.execute("""
                INSERT IGNORE INTO cps_freeze_config
                  (platform_code, unfreeze_days, status, remark, tenant_id, creator)
                VALUES (%s,%s,1,%s,1,'admin')
            """, (platform_code, days, remark))
        print(f'  ✓ cps_freeze_config 已有全局配置，补充平台专属3条')

    conn.commit()
    cursor.close()


# ──────────────────────────────────────────────
# STEP 5: 订单 + 返利记录 + 会员账户
# ──────────────────────────────────────────────
def step5_orders_rebate(conn):
    print('\n========== STEP 5: 订单 & 返利记录 & 账户 ==========')
    cursor = conn.cursor()
    cursor.execute("DELETE FROM cps_order WHERE platform_order_id LIKE 'TEST_%'")
    cursor.execute("DELETE FROM cps_rebate_record WHERE platform_order_id LIKE 'TEST_%'")
    cursor.execute("DELETE FROM cps_rebate_account WHERE member_id IN (1001,1002,1003)")

    # 5 条覆盖全部订单状态的测试数据
    orders = [
        # (platform_code, platform_order_id, member_id, item_id, item_title,
        #  item_price, final_price, coupon_amount, commission_rate, commission_amount,
        #  estimate_rebate, real_rebate, adzone_id, external_info, order_status, rebate_freeze_status)
        ('taobao', 'TEST_TB_20260101_001', 1001, '123456789', 'Apple iPhone 16 手机壳 防摔硅胶',
         39.90, 19.90, 20.00, 0.2000, 3.98, 3.38, 0.00,
         'mm_taobao_default_pid', '1001', 'created', 'pending'),

        ('taobao', 'TEST_TB_20260102_002', 1001, '987654321', '耐克运动鞋 男款 2026春季新款',
         599.00, 399.00, 200.00, 0.1500, 59.85, 50.87, 50.87,
         'mm_taobao_member_1001', '1001', 'received', 'frozen'),

        ('jd', 'TEST_JD_20260103_003', 1002, '100012345678', '戴森V15吸尘器 家用无线',
         3999.00, 3299.00, 700.00, 0.0800, 263.92, 216.41, 216.41,
         'jd_default_unionid', '1002', 'settled', 'unfreezed'),

        ('pdd', 'TEST_PDD_20260104_004', 1002, 'pdd_goods_sign_abc', '老干妈辣椒酱280g*6瓶装',
         59.90, 29.90, 30.00, 0.1200, 3.59, 2.87, 2.87,
         'pdd_default_pid', '1002', 'rebate_received', 'unfreezed'),

        ('taobao', 'TEST_TB_20260105_005', 1003, '555666777', '小米14Pro 手机 骁龙8Gen3',
         4999.00, 4599.00, 400.00, 0.0500, 229.95, 195.46, 0.00,
         'mm_taobao_default_pid', '1003', 'refunded', 'pending'),
    ]

    order_ids = {}
    for o in orders:
        cursor.execute("""
            INSERT INTO cps_order
              (platform_code, platform_order_id, member_id, item_id, item_title,
               item_price, final_price, coupon_amount, commission_rate, commission_amount,
               estimate_rebate, real_rebate, adzone_id, external_info, order_status,
               rebate_freeze_status, tenant_id, creator)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,1,'system')
        """, o)
        order_ids[o[1]] = cursor.lastrowid
    print(f'  ✓ cps_order 插入 {len(orders)} 条')

    rebate_records = [
        (1001, order_ids.get('TEST_TB_20260102_002'), 'taobao', 'TEST_TB_20260102_002',
         '987654321', '耐克运动鞋', 399.00, 59.85, 0.8500, 50.87, 'rebate', 'pending', '已收货待冻结解冻'),

        (1002, order_ids.get('TEST_JD_20260103_003'), 'jd', 'TEST_JD_20260103_003',
         '100012345678', '戴森V15吸尘器', 3299.00, 263.92, 0.8200, 216.41, 'rebate', 'Rcptd', '已结算并入账'),

        (1002, order_ids.get('TEST_PDD_20260104_004'), 'pdd', 'TEST_PDD_20260104_004',
         'pdd_goods_sign_abc', '老干妈辣椒酱', 29.90, 3.59, 0.8000, 2.87, 'rebate', 'Rcptd', '已入账'),
    ]
    for rr in rebate_records:
        cursor.execute("""
            INSERT INTO cps_rebate_record
              (member_id, order_id, platform_code, platform_order_id,
               item_id, item_title, order_amount, commission_amount,
               rebate_rate, rebate_amount, rebate_type, rebate_status,
               remark, tenant_id, creator)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,1,'system')
        """, rr)
    print(f'  ✓ cps_rebate_record 插入 {len(rebate_records)} 条')

    accounts = [
        # (member_id, total_rebate, available_balance, frozen_balance, withdrawn_amount)
        (1001, 50.87,  50.87,  50.87, 0.00),   # 1001: 有冻结余额
        (1002, 219.28, 219.28,  0.00, 0.00),   # 1002: 可用余额
        (1003,  0.00,   0.00,   0.00, 0.00),   # 1003: 退款无返利
    ]
    for acc in accounts:
        cursor.execute("""
            INSERT INTO cps_rebate_account
              (member_id, total_rebate, available_balance, frozen_balance,
               withdrawn_amount, status, version, tenant_id, creator)
            VALUES (%s,%s,%s,%s,%s,1,0,1,'system')
        """, acc)
    print(f'  ✓ cps_rebate_account 插入 {len(accounts)} 条')

    conn.commit()
    cursor.close()


# ──────────────────────────────────────────────
# STEP 6: MCP API Key + 转链记录
# ──────────────────────────────────────────────
def step6_mcp_transfer(conn):
    print('\n========== STEP 6: MCP Key & 转链记录 ==========')
    cursor = conn.cursor()
    cursor.execute("DELETE FROM cps_mcp_api_key WHERE name LIKE 'TEST_%'")
    cursor.execute("DELETE FROM cps_transfer_record WHERE member_id IN (1001,1002,1003)")

    mcp_keys = [
        ('TEST_AI_AGENT_KEY', 'cps-mcp-test-key-ai-agent-2026-000001',  'AI Agent测试密钥（永久）', None),
        ('TEST_CURSOR_KEY',   'cps-mcp-test-key-cursor-client-2026-002', 'Cursor接入测试密钥',       None),
        ('TEST_EXPIRED_KEY',  'cps-mcp-test-key-expired-2025-003',       '已过期测试密钥',          '2025-01-01 00:00:00'),
    ]
    for k in mcp_keys:
        cursor.execute("""
            INSERT INTO cps_mcp_api_key
              (name, key_value, description, expire_time, status, tenant_id, creator)
            VALUES (%s,%s,%s,%s,1,1,'admin')
        """, k)
    print(f'  ✓ cps_mcp_api_key 插入 {len(mcp_keys)} 条')

    transfers = [
        (1001, 'taobao', 'https://item.taobao.com/item.htm?id=123456789',
         '123456789', 'Apple iPhone 16 手机壳',
         'https://uland.taobao.com/coupon/edetail?pid=mm_taobao_default_pid&id=123456789',
         '【淘宝】iOhf...', 'mm_taobao_default_pid'),
        (1001, 'taobao', 'https://item.taobao.com/item.htm?id=987654321',
         '987654321', '耐克运动鞋',
         'https://uland.taobao.com/coupon/edetail?pid=mm_taobao_member_1001&id=987654321',
         '【淘宝】Nike...', 'mm_taobao_member_1001'),
        (1002, 'jd', 'https://item.jd.com/100012345678.html',
         '100012345678', '戴森V15吸尘器',
         'https://union-click.jd.com/jdc?e=&p=AyIGZRtfEjsS',
         None, 'jd_default_unionid'),
        (1002, 'pdd', 'https://mobile.yangkeduo.com/goods.html?goods_id=999001',
         'pdd_goods_sign_abc', '老干妈辣椒酱',
         'https://mobile.yangkeduo.com/promotion/coupon/goods.html?pid=pdd_default_pid',
         None, 'pdd_default_pid'),
    ]
    for tr in transfers:
        cursor.execute("""
            INSERT INTO cps_transfer_record
              (member_id, platform_code, original_content, item_id, item_title,
               promotion_url, tao_command, adzone_id, status, tenant_id, creator)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s,1,1,'system')
        """, tr)
    print(f'  ✓ cps_transfer_record 插入 {len(transfers)} 条')

    conn.commit()
    cursor.close()


# ──────────────────────────────────────────────
# MAIN
# ──────────────────────────────────────────────
def main():
    print('=' * 55)
    print('  CPS 完整测试数据初始化脚本')
    print('  连接: 127.0.0.1:3306  库: cps')
    print('  SQL : backend/sql/module/cps-all-in-one.sql')
    print('=' * 55)

    conn = get_conn()
    print('✓ 数据库连接成功')

    try:
        step1_schema(conn)
        step2_platform_vendor(conn)
        step3_adzone(conn)
        step4_rebate_freeze_config(conn)
        step5_orders_rebate(conn)
        step6_mcp_transfer(conn)
        check_tables(conn)
        print('\n✅ 所有测试数据执行完成！')
    except Exception as e:
        conn.rollback()
        print(f'\n❌ 执行失败: {e}')
        import traceback
        traceback.print_exc()
        sys.exit(1)
    finally:
        conn.close()


if __name__ == '__main__':
    main()

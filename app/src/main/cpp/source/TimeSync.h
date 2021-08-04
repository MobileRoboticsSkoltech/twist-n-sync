//
// Created by achains on 18.07.2021.
//

#ifndef TWIST_N_SYNC_CPP_MODULE_TIMESYNC_H
#define TWIST_N_SYNC_CPP_MODULE_TIMESYNC_H

#include <scapix/bridge/object.h>
#include "Eigen/Core"
#include "util/TSUtil.h"
#include <vector>

class TimeSync : public scapix::bridge::object<TimeSync> {
 public:
    explicit TimeSync(std::vector<std::vector<double>> const & gyro_first,
                      std::vector<std::vector<double>> const & gyro_second,
                      std::vector<double> const & ts_first,
                      std::vector<double> const & ts_second,
                      bool const & do_resample = true);

    void obtainDelay();

    void resample(double const & accuracy);

    double getTimeDelay() const;

 private:

    static Eigen::MatrixX3d & interpolateGyro(Eigen::VectorXd const & ts_old, Eigen::MatrixX3d const & gyro_old,
                                            Eigen::VectorXd const & ts_new, Eigen::MatrixX3d & gyro_new);

    static Eigen::Vector2d obtainRoots(Eigen::VectorXd const & coeffs, Eigen::Index const & order);

    TSUtil::CorrData getInitialIndex() const;

    // 3D angular velocities from devices' gyros
    Eigen::MatrixX3d gyro_first_;
    Eigen::MatrixX3d gyro_second_;

    // Gyros' timestamps
    Eigen::VectorXd ts_first_;
    Eigen::VectorXd ts_second_;

    double dt_ = 0.0;

    // Flag to do resampling of angular velocities
    bool do_resample_;
    double time_delay_ = 0.0;
};


#endif //TWIST_N_SYNC_CPP_MODULE_TIMESYNC_H
